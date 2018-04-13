package com.r3.corda.finance.cash.issuer.service.flows

import com.r3.corda.finance.cash.issuer.common.utilities.getNostroAccountBalances
import net.corda.core.flows.FlowLogic

class GetNostroAccountBalances : FlowLogic<Map<String, Long>>() {
    override fun call() = getNostroAccountBalances(serviceHub)
}