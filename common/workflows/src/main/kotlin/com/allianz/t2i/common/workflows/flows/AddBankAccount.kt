package com.allianz.t2i.common.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.contracts.BankAccountContract
import com.allianz.t2i.common.contracts.types.BankAccount
import com.allianz.t2i.common.contracts.types.toState
import com.allianz.t2i.common.workflows.utilities.getBankAccountStateByAccountNumber
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Adds a new [BankAccount] state to the ledger.
 * This assumes that the [BankAccount] will only ever need to be shared with one verifier/issuer. There are no flows to
 * share an existing [BankAccount] with another issuer (they were deleted in a previous PR). However it is likely that
 * the same bank account state might need to be shared with more than one issuer.
 */
@StartableByRPC
@StartableByService
@InitiatingFlow
class AddBankAccount(val bankAccount: BankAccount, val verifier: Party) : FlowLogic<SignedTransaction>() {

    // TODO: Add the rest of the progress tracker.
    companion object {
        object FINALISING : ProgressTracker.Step("Finalising transaction.")

        fun tracker() = ProgressTracker(FINALISING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Starting AddBankAccount flow...")
        val accountNumber = bankAccount.accountNumber

        logger.info("Checking for existence of state for $bankAccount.")
        val result = getBankAccountStateByAccountNumber(accountNumber, serviceHub)

        if (result != null) {
            val linearId = result.state.data.linearId
            throw IllegalArgumentException("Bank account $accountNumber already exists with linearId ($linearId).")
        }

        logger.info("No state for $bankAccount. Adding it.")
        val bankAccountState = bankAccount.toState(ourIdentity, verifier)
        logger.info("Bank account state that is added: $bankAccountState")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // The node running this flow is always the only signer.
        val command = Command(BankAccountContract.Add(), listOf(ourIdentity.owningKey))
        val unsignedTransaction = TransactionBuilder(notary = notary).apply {
            addOutputState(bankAccountState)
            addCommand(command)
        }

        // Sign the transaction with legal identity key.
        val signedTransaction = serviceHub.signInitialTransaction(unsignedTransaction)

        progressTracker.currentStep = FINALISING
        // If the verifier IS the node running this flow then don't initiate a session for FinalityFlow.
        val verifierSession = initiateFlow(bankAccountState.verifier)
        val sessionsForFinality = if (serviceHub.myInfo.isLegalIdentity(verifier)) emptyList() else listOf(verifierSession)
        return subFlow(FinalityFlow(signedTransaction, sessionsForFinality))
    }

}