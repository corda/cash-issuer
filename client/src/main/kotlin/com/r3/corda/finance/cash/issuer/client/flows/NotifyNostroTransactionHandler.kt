package com.r3.corda.finance.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.flows.AbstractNotifyNostroTransaction
import com.r3.corda.finance.cash.issuer.common.types.NostroTransaction
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.unwrap

@InitiatedBy(AbstractNotifyNostroTransaction::class)
class NotifyNostroTransactionHandler(val otherSession: FlowSession) : FlowLogic<NostroTransaction>() {
    @Suspendable
    override fun call(): NostroTransaction {
        return otherSession.receive<NostroTransaction>().unwrap { it }
    }
}