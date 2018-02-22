package com.r3.bn.user.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.bn.common.flows.AbstractJoinBusinessNetworkRequest
import com.r3.bn.common.states.MembershipRecord
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

/**
 * We use a Party object here as the node shell will fuzzy match on X500 names, making testing less cumbersome.
 */
@StartableByRPC
class JoinBusinessNetworkRequest(val bno: Party) : AbstractJoinBusinessNetworkRequest() {

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
        // Create and sign a new join request.
        val unsignedTransaction = MembershipRecord.createNewRecord(bno, ourIdentity, services = serviceHub)
        val partiallySignedTransaction = serviceHub.signInitialTransaction(unsignedTransaction)
        val session = initiateFlow(bno)

        // Sign and distribute the join request.
        progressTracker.currentStep = COLLECTING
        val fullySignedTransaction = subFlow(CollectSignaturesFlow(
                partiallySignedTx = partiallySignedTransaction,
                sessionsToCollectFrom = listOf(session),
                progressTracker = COLLECTING.childProgressTracker())
        )

        progressTracker.currentStep = FINALISING
        val finalisedTransaction = subFlow(FinalityFlow(fullySignedTransaction, FINALISING.childProgressTracker()))

        // At this point, the BNO would likely send the signed CorDapp binaries to the requesting party (the caller of
        // this flow). For now though, we'll just receive a "Unit" and pretend it represents the CorDapp binaries.
        // TODO: Implement sending of the CorDapp JAR over the flow framework.
        session.receive<Unit>()

        // Return the MembershipRecord state confirming the request to join the business network.
        return finalisedTransaction
    }

}