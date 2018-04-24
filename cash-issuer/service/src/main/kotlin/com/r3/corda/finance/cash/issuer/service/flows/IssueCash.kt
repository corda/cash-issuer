package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.contracts.CashContract
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.states.NostroTransactionState
import com.r3.corda.finance.cash.issuer.service.contracts.NodeTransactionContract
import com.r3.corda.finance.cash.issuer.service.states.NodeTransactionState
import net.corda.core.contracts.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.finance.contracts.asset.Cash
import java.time.Instant

// TODO: Denominations? E.g. 100 split between 10 issuances of 10.
// TODO: Ask the counterparty for a random key and issue to that key.
// TODO: Move all tx generation stuff to the common library.

@StartableByService
class IssueCash(val stx: SignedTransaction) : FlowLogic<Pair<SignedTransaction, SignedTransaction>>() {

    @Suspendable
    override fun call(): Pair<SignedTransaction, SignedTransaction> {
        // Our chosen notary.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // The original issuance details.
        val counterparty = stx.tx.toLedgerTransaction(serviceHub).referenceStates.single {
            (it as BankAccountState).owner != ourIdentity
        } as BankAccountState
        val nostroTransactionStateAndRef = stx.tx.outRefsOfType<NostroTransactionState>().single()
        val issuanceAmount = nostroTransactionStateAndRef.state.data.amountTransfer

        /** Commit the internal only transactions. */
        // A record of the issuance for the issuer. We store this separately to the nostro transaction states as these
        // records pertain to issuances and redemptions of cash states as opposed to payments in and out of the nostro
        // accounts. Currently this state is committed to the ledger separetely to the cash issuance. Ideally we want to
        // commit them atomically.
        // TODO: Update to commit all states atomically whilst preserving confidentiality of the issuer's workflows.
        val nodeTransactionState = NodeTransactionState(
                amountTransfer = AmountTransfer(
                        quantityDelta = issuanceAmount.quantityDelta,
                        token = issuanceAmount.token,
                        source = ourIdentity,
                        destination = counterparty.owner
                ),
                createdAt = Instant.now(),
                participants = listOf(ourIdentity)
        )

        // TODO: Add node transaction contract code to check info.
        val internalBuilder = TransactionBuilder(notary = notary)
                .addReferenceState(nostroTransactionStateAndRef.referenced())
                .addOutputState(nodeTransactionState, NodeTransactionContract.CONTRACT_ID)
                .addCommand(NodeTransactionContract.Create(), listOf(ourIdentity.owningKey))

        val internalStx = serviceHub.signInitialTransaction(internalBuilder)
        val internfalFtx = subFlow(FinalityFlow(internalStx))

        /** Commit the cash issuance transaction. */
        // Issue command.
        val command = Command(Cash.Commands.Issue(), listOf(ourIdentity.owningKey))

        // Create a new cash state.
        // The issuer reference is the hash of the transaction containing the nostro transaction state which led to
        // this issuance. We include this so we can check it during redemption time. Using this as the reference means
        // that cash states won't get merged very often as it's likely that the reference is different (one for each
        // issuance).
        val partyAndReference = PartyAndReference(ourIdentity, internfalFtx.id)
        val issuerAndToken = Issued(partyAndReference, issuanceAmount.token)
        val amount = Amount(issuanceAmount.quantityDelta, issuerAndToken)
        val cashState = Cash.State(
                amount = amount,
                owner = counterparty.owner
        )

        val externalBuilder = TransactionBuilder(notary = notary)
                .addOutputState(cashState, CashContract.CONTRACT_ID)
                .addCommand(command)

        // Sign and finalise.
        val externalStx = serviceHub.signInitialTransaction(externalBuilder)
        val externalFtx = subFlow(FinalityFlow(externalStx))
        return Pair(internfalFtx, externalFtx)
    }

}