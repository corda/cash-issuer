package com.r3.corda.finance.cash.issuer.common.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.contracts.BankAccountContract
import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.r3.corda.finance.cash.issuer.common.types.toState
import com.r3.corda.finance.cash.issuer.common.utilities.getBankAccountStateByAccountNumber
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * Adds a new [BankAccount] state to the ledger.
 * The state is alwats added as a "uni-lateral state" to the node calling this flow.
 */
object AddBankAccountFlow {

    @StartableByRPC
    @StartableByService
    @InitiatingFlow
    class AddBankAccount(val bankAccount: BankAccount, val verifier: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            // TODO: Add the rest of the progress tracker.
            object FINALISING : ProgressTracker.Step("Finalising transaction.")

            @JvmStatic
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
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val verifierSsession = initiateFlow(bankAccountState.verifier)

            // The node running this flow is always the only signer.
            val command = Command(BankAccountContract.Add(), listOf(ourIdentity.owningKey))
            val outputStateAndContract = StateAndContract(bankAccountState, BankAccountContract.CONTRACT_ID)
            val unsignedTransaction = TransactionBuilder(notary = notary).withItems(command, outputStateAndContract)

            val signedTransaction = serviceHub.signInitialTransaction(unsignedTransaction)

            progressTracker.currentStep = FINALISING
            // Share the added bank account state with the verifier/issuer.
            val sessionsForFinality = if (serviceHub.myInfo.isLegalIdentity(bankAccountState.verifier)) emptyList() else listOf(verifierSsession)
            return subFlow(FinalityFlow(signedTransaction, sessionsForFinality))
        }

    }

    @InitiatedBy(AddBankAccount::class)
    class ReceiveBankAccount(val otherSession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            logger.info("Starting ReceiveBankAccount flow...")
            //return subFlow(ReceiveTransactionFlow(otherSession, true, StatesToRecord.ALL_VISIBLE))
            if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
                subFlow(ReceiveFinalityFlow(otherSession))
            }
        }
    }
}