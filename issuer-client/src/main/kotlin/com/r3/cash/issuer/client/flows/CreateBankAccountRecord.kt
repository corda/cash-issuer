package com.r3.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.cash.issuer.common.flows.AbstractCreateBankAccountRecord
import com.r3.cash.issuer.common.states.BankAccountRecord
import com.r3.cash.issuer.common.types.AccountNumber
import com.r3.cash.issuer.common.types.SortCode
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class CreateBankAccountRecord(
        val issuer: Party,
        val accountName: String,
        val accountNumber: String,
        val sortCode: String
) : AbstractCreateBankAccountRecord() {

    companion object {
        // TODO: Add the rest of the progress tracker.
        object COLLECTING : ProgressTracker.Step("Collecting counterparty signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING : ProgressTracker.Step("Finalising transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(COLLECTING, FINALISING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val me = serviceHub.myInfo.legalIdentities.first()
        val unsignedTransaction = BankAccountRecord.createNewBankAccountRecord(
                accountOwner = me,
                issuer = issuer,
                accountName = accountName,
                accountNumber = AccountNumber(accountNumber),
                sortCode = SortCode(sortCode),
                services = serviceHub
        )

        val partiallySignedTransaction = serviceHub.signInitialTransaction(unsignedTransaction)
        val session = initiateFlow(issuer)
        // Sign and distribute the join request.
        progressTracker.currentStep = COLLECTING
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(
                partiallySignedTx = partiallySignedTransaction,
                sessionsToCollectFrom = listOf(session),
                progressTracker = COLLECTING.childProgressTracker())
        )
        progressTracker.currentStep = FINALISING
        return subFlow(FinalityFlow(fullySignedTransaction, FINALISING.childProgressTracker()))
    }

}