package com.allianz.t2i.issuer.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.workflows.utilities.getLatestNostroTransactionStatesGroupedByAccount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class GetLastUpdatesByAccountId : FlowLogic<Map<String, Long>>() {
    @Suspendable
    override fun call() = getLatestNostroTransactionStatesGroupedByAccount(serviceHub)
}