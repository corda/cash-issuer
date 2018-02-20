package com.r3.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.cash.issuer.base.flows.UpdateBusinessNetworkMembershipStatus
import com.r3.cash.issuer.base.flows.UpdateBusinessNetworkMembershipStatusHandler
import com.r3.cash.issuer.base.types.RecordStatusUpdateNotification
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.unwrap

@InitiatedBy(UpdateBusinessNetworkMembershipStatus::class)
class UpdateRecordStatusHandler(otherSession: FlowSession) : UpdateBusinessNetworkMembershipStatusHandler(otherSession) {

    @Suspendable
    override fun call() {
        val response = otherSession.receive<RecordStatusUpdateNotification>().unwrap { it }
        logger.info("Membership status updated from ${response.previousStatus} to ${response.newStatus}.")
        // TODO: Println included for demoing via the console.
        println("\nMembership status updated from ${response.previousStatus} to ${response.newStatus}. PRESS ENTER TO CONTINUE.")
    }

}