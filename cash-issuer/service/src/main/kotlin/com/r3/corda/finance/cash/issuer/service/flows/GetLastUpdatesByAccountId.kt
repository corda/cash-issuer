package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.utilities.getLatestNostroTransactionStatesGroupedByAccount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class GetLastUpdatesByAccountId : FlowLogic<Map<String, Long>>() {
    @Suspendable
    override fun call() = getLatestNostroTransactionStatesGroupedByAccount(serviceHub)
}