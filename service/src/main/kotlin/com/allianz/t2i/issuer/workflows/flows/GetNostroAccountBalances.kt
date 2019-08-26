package com.allianz.t2i.issuer.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.workflows.utilities.getNostroAccountBalances
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class GetNostroAccountBalances : FlowLogic<Map<String, Long>>() {
    @Suspendable
    override fun call() = getNostroAccountBalances(serviceHub)
}