package com.r3.cash.issuer.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.cash.issuer.base.flows.UpdateBusinessNetworkMembershipStatus
import com.r3.cash.issuer.base.types.RecordStatus
import com.r3.cash.issuer.base.types.RecordStatusUpdateNotification
import com.r3.cash.issuer.services.InMemoryMembershipService
import net.corda.core.flows.FlowException
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class UpdateRecordStatus(
        val member: Party,
        val newStatus: RecordStatus
) : UpdateBusinessNetworkMembershipStatus() {

    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val recordKey = member.name
        val session = initiateFlow(member)
        val membershipService = serviceHub.cordaService(InMemoryMembershipService::class.java)

        if (membershipService.hasRecord(recordKey).not()) {
            throw FlowException("$member is not on the membership list.")
        }

        val previousRecord = try {
            membershipService.updateRecord(recordKey, newStatus)
        } catch (e: IllegalArgumentException) {
            throw FlowException(e)
        }

        logger.info("UpdateRecordStatus: $member status updated to $newStatus")
        // TODO: This println is temporary for demos using the node shell.
        println("$member status updated to $newStatus.")

        session.send(RecordStatusUpdateNotification(previousRecord.status, newStatus))
    }

}