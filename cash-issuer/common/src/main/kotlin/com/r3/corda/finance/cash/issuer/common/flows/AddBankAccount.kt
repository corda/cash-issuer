package com.r3.corda.finance.cash.issuer.common.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.contracts.BankAccountContract
import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.r3.corda.finance.cash.issuer.common.types.toState
import com.r3.corda.finance.cash.issuer.common.utilities.getBankAccountStateByAccountNumber
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
@StartableByService
class AddBankAccount(val bankAccount: BankAccount) : FlowLogic<SignedTransaction>() {

    companion object {
        // TODO: Add the rest of the progress tracker.
        object FINALISING : ProgressTracker.Step("Finalising transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(FINALISING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val accountNumber = bankAccount.accountNumber
        val result = getBankAccountStateByAccountNumber(accountNumber, serviceHub)

        if (result != null) {
            val linearId = result.state.data.linearId
            throw IllegalArgumentException("Bank account $accountNumber already exists with linearId ($linearId).")
        }

        logger.info("Adding $bankAccount.")
        val bankAccountState = bankAccount.toState(ourIdentity)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // The node running this flow is always the only signer.
        val command = Command(BankAccountContract.Add(), listOf(ourIdentity.owningKey))
        val outputStateAndContract = StateAndContract(bankAccountState, BankAccountContract.CONTRACT_ID)
        val unsignedTransaction = TransactionBuilder(notary = notary).withItems(command, outputStateAndContract)

        val signedTransaction = serviceHub.signInitialTransaction(unsignedTransaction)

        progressTracker.currentStep = FINALISING
        return subFlow(FinalityFlow(signedTransaction, FINALISING.childProgressTracker()))
    }

}