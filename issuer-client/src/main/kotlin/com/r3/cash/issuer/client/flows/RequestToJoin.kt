package com.r3.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.cash.issuer.base.flows.JoinBusinessNetworkRequest
import com.r3.cash.issuer.base.types.RequestToJoinBusinessNetworkResponse
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@StartableByRPC
class RequestToJoin(val issuer: Party) : JoinBusinessNetworkRequest() {

    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call(): RequestToJoinBusinessNetworkResponse {
        val session = initiateFlow(issuer)
        val response = session.sendAndReceive<RequestToJoinBusinessNetworkResponse>(Unit).unwrap { it }
        println("Request to join response received: $response.")
        return response
    }

}