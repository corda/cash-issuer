package com.r3.cash.issuer.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.cash.issuer.base.flows.JoinBusinessNetworkRequest
import com.r3.cash.issuer.base.flows.JoinBusinessNetworkRequestHandler
import com.r3.cash.issuer.base.types.RequestToJoinBusinessNetworkResponse
import com.r3.cash.issuer.services.InMemoryMembershipService
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.ProgressTracker

@InitiatedBy(JoinBusinessNetworkRequest::class)
class RequestToJoin(otherSession: FlowSession) : JoinBusinessNetworkRequestHandler(otherSession) {

    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val requestingNode = otherSession.counterparty

        otherSession.receive<Unit>()
        logger.info("Request to join the cash business network received from $requestingNode.")
        val membershipService = serviceHub.cordaService(InMemoryMembershipService::class.java)
        val recordKey = requestingNode.name

        if (membershipService.hasRecord(requestingNode.name)) {
            // There is already a record for this node, get it and return it back to the requesting node.
            logger.info("RequestToJoin: $requestingNode's request failed.")
            val membershipData = membershipService.getRecord(recordKey)!!
            otherSession.send(RequestToJoinBusinessNetworkResponse.Failure(membershipData))
        } else {
            // There is no record for this node.
            logger.info("RequestToJoin: $requestingNode's request succeeded.")
            membershipService.addNewRecord(recordKey)
            otherSession.send(RequestToJoinBusinessNetworkResponse.Success())
        }
    }

}