package com.r3.bn.operator.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.bn.common.flows.AbstractUpdateMembershipRecordStatus
import com.r3.bn.common.states.MembershipRecord
import com.r3.bn.common.types.MembershipStatus
import com.r3.bn.operator.getMemberByX500Name
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class UpdateMembershipRecordStatus(
        val member: Party,
        val newStatus: MembershipStatus
) : AbstractUpdateMembershipRecordStatus() {

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
        val currentMembershipRecord = getMemberByX500Name(member.name, serviceHub)
                ?: throw IllegalArgumentException("Record for $member not found.")
        val unsignedTransaction = MembershipRecord.updateRecordStatus(currentMembershipRecord, newStatus)
        val fullySignedTransaction = serviceHub.signInitialTransaction(unsignedTransaction)
        progressTracker.currentStep = FINALISING
        return subFlow(FinalityFlow(fullySignedTransaction, FINALISING.childProgressTracker()))
    }

}