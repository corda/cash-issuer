package com.r3.cash.issuer.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.cash.issuer.base.types.RecordData
import com.r3.cash.issuer.base.types.RecordStatus
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class ListRequests : FlowLogic<Map<CordaX500Name, RecordData>>() {

    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Map<CordaX500Name, RecordData> {
        val currentRequests = subFlow(ListRecordsByStatus(RecordStatus.REQUESTED))
        // TODO: This println is temporary for demos using the node shell.
        currentRequests.forEach(::println)
        return currentRequests
    }

}