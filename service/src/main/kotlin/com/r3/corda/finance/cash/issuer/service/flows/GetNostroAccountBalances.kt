package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.utilities.getNostroAccountBalances
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class GetNostroAccountBalances : FlowLogic<Map<String, Long>>() {
    @Suspendable
    override fun call() = getNostroAccountBalances(serviceHub)
}