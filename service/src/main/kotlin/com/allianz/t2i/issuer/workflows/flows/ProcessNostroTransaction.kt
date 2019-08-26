package com.allianz.t2i.issuer.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.contracts.NodeTransactionContract
import com.allianz.t2i.common.contracts.NostroTransactionContract
import com.allianz.t2i.common.contracts.states.BankAccountState
import com.allianz.t2i.common.contracts.states.NodeTransactionState
import com.allianz.t2i.common.contracts.states.NostroTransactionState
import com.allianz.t2i.common.contracts.types.NoAccountNumber
import com.allianz.t2i.common.contracts.types.NodeTransactionType
import com.allianz.t2i.common.contracts.types.NostroTransactionStatus
import com.allianz.t2i.common.contracts.types.NostroTransactionType
import com.allianz.t2i.common.workflows.utilities.getBankAccountStateByAccountNumber
import com.allianz.t2i.common.workflows.utilities.getPendingRedemptionByNotes
import net.corda.core.contracts.AmountTransfer
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

// TODO: What about segregation of duties? Typically, something like an issuance event would require multiple sign-offs.
// Perhaps this can be done for transactions over a certain value.

/**
 * This takes a nostro transaction states and attempts to match it to two bank account state.
 * If we can get a match then, in theory, we know what to do in respect of this transaction hitting the nostro account.
 * If we can't get a match we need to triage the nostro transaction and figure out what to do with it.
 * TODO This flow should probably be called MatchNostroTransactionFlow
 */
@StartableByService
@InitiatingFlow
class ProcessNostroTransaction(val stateAndRef: StateAndRef<NostroTransactionState>) : FlowLogic<SignedTransaction>() {

    /**
     * Updates the status of the nostro transaction state. Doesn't add anything else. No return type as the builder is
     * mutable.
     */
    @Suspendable
    private fun createBaseTransaction(builder: TransactionBuilder, newType: NostroTransactionType, newStatus: NostroTransactionStatus) {
        val command = Command(NostroTransactionContract.Match(), listOf(ourIdentity.owningKey))
        val nostroTransactionOutput = stateAndRef.state.data.copy(type = newType, status = newStatus)
        builder
                .addInputState(stateAndRef)
                .addCommand(command)
                .addOutputState(nostroTransactionOutput, NostroTransactionContract.CONTRACT_ID)
    }

    @Suspendable
    private fun addNodeTransactionState(
            builder: TransactionBuilder,
            bankAccountStates: List<StateAndRef<BankAccountState>>,
            nostroTransactionState: NostroTransactionState,
            isRedemption: Boolean = false
    ) {
        // The original issuance details.
        val counterparty = bankAccountStates.single { it.state.data.owner != ourIdentity }.state.data.owner
        val issuanceAmount = nostroTransactionState.amountTransfer
        // A record of the issuance for the issuer. We store this separately to the nostro transaction states as these
        // records pertain to issuance and redemption of cash states as opposed to payments in and out of the nostro
        // accounts. Currently this state is committed to the ledger separately to the cash issuance. Ideally we want to
        // commit them atomically.
        // TODO: This is a hack which needs removing.
        // Node transaction states for redemptions are only added by the redeem cash handler. So.. if we remove some
        // data from the node (accidentally perhaps) then when the node processes the nostro transactions
        val nodeTransactionState = NodeTransactionState(
                amountTransfer = AmountTransfer(
                        quantityDelta = issuanceAmount.quantityDelta,
                        token = issuanceAmount.token,
                        source = if (isRedemption) counterparty else ourIdentity,
                        destination = if (isRedemption) ourIdentity else counterparty
                ),
                notes = nostroTransactionState.description,
                createdAt = Instant.now(),
                participants = listOf(ourIdentity),
                type = if (isRedemption) NodeTransactionType.REDEMPTION else NodeTransactionType.ISSUANCE
        )

        // TODO: Add node transaction contract code to check info.
        builder.addOutputState(nodeTransactionState, NodeTransactionContract.CONTRACT_ID)
                .addCommand(NodeTransactionContract.Create(), listOf(ourIdentity.owningKey))
    }

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Starting ProcessNostroTransaction flow.")
        // For brevity.
        val nostroTransaction = stateAndRef.state.data
        val amountTransfer = nostroTransaction.amountTransfer

        // Get StateAndRefs for the bank account data. We discard the nulls. This will contain either 0, 1 or 2 bank
        // account state refs. If there's three or more then we have a dupe and this should never happen.
        val bankAccountStateRefs = listOf(amountTransfer.source, amountTransfer.destination).map { accountNumber ->
            if (accountNumber !is NoAccountNumber) {
                getBankAccountStateByAccountNumber(accountNumber, serviceHub)
            } else null
        }.filterNotNull()

        // Set up our transaction builder.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary = notary)

        // It's easier to work with ContractStates.
        val bankAccountStates = bankAccountStateRefs.map { it.state.data }

        /** ONLY ONE OF THESE CONDITIONS SHOULD EVER BE TRUE FOR EACH NOSTRO TRANSACTION! */

        // If there are no matches then something has gone quite wrong.
        // We always should have, at least, the bank account details for the issuer.
        val areNoMatches = bankAccountStates.isEmpty()

        // We can only match one of the bank accounts.
        // If there is only one account matched then it should always be the issuer's account.
        val isSingleMatch = bankAccountStates.size == 1
        val issuerMatchOnly = bankAccountStates.singleOrNull { it.owner == ourIdentity } != null && isSingleMatch

        // If all of the bank accounts are the issuer's then this transaction must be a
        // transfer between nostro accounts. We should see an equal and opposite transfer
        // on another account.
        val isDoubleMatch = bankAccountStates.size == 2
        val isDoubleMatchInternalTransfer = bankAccountStates.all { it.owner == ourIdentity } && isDoubleMatch

        // Check to see if one of the accounts is ours and the other, a counterparty's.
        val singleIssuerBankAccount = bankAccountStates.singleOrNull { it.owner == ourIdentity }
        val singleCounterpartyBankAccount = bankAccountStates.toSet().minus(singleIssuerBankAccount).singleOrNull()
        val isDoubleMatchExternalTransfer = singleIssuerBankAccount != null && singleCounterpartyBankAccount != null

        // If the amount transfer is greater than zero, then the assumption is that if it isn't an
        // internal transfer, it MUST be a deposit from a counterparty's account. Therefore, an issuance.
        val isIssuance = amountTransfer.quantityDelta > 0L && (isDoubleMatchExternalTransfer || issuerMatchOnly)
        val isRedemption = amountTransfer.quantityDelta < 0L && (isDoubleMatchExternalTransfer || issuerMatchOnly)

        // Add whatever nostro account states we have in the list.
        bankAccountStateRefs.forEach { builder.addReferenceState(it.referenced()) }

        when {
            areNoMatches -> throw FlowException("We should always, at least, have our bank account data recorded.")
            isDoubleMatchInternalTransfer -> {
                logger.info("The nostro transaction is an internal transfer.")
                createBaseTransaction(builder, NostroTransactionType.COLLATERAL_TRANSFER, NostroTransactionStatus.MATCHED)
            }
            issuerMatchOnly -> {
                logger.info("We don't have the counterparty's bank account details.")
                logger.info("We'll have to keep this cash safe until we figure out who sent it to us.")
                val type = if (isIssuance) NostroTransactionType.ISSUANCE else NostroTransactionType.REDEMPTION
                createBaseTransaction(builder, type, NostroTransactionStatus.MATCHED_ISSUER)
            }
            isIssuance -> {
                createBaseTransaction(builder, NostroTransactionType.ISSUANCE, NostroTransactionStatus.MATCHED)
                addNodeTransactionState(builder, bankAccountStateRefs, nostroTransaction, isRedemption)
                // TODO: Check that accounts are verified.
                logger.info("This is an issuance!")
            }
            isRedemption -> {
                createBaseTransaction(builder, NostroTransactionType.REDEMPTION, NostroTransactionStatus.MATCHED)
                // TODO: Hack alert!!! (Need to do some more thinking around this re: "start from date").
                // Need to work out how we deal with backup restores or processing nostro transactinos which have
                // already been processed after a database backup restore. Currently, I'm just re-creating the pending
                // node transaction state here as the assumption is that node transaction states always precede
                // nostro transaction states for redemptions.
                if (getPendingRedemptionByNotes(nostroTransaction.description, serviceHub) == null) {
                    addNodeTransactionState(builder, bankAccountStateRefs, nostroTransaction, isRedemption)
                }
                logger.info("This is an redemption!")
            }
            else -> throw FlowException("Something went wrong. Someone is going to be in trouble...!")
        }

        val stx = serviceHub.signInitialTransaction(builder)
        return subFlow(FinalityFlow(stx, emptySet<FlowSession>()))
    }

}