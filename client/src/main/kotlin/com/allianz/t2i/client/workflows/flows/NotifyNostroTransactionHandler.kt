package com.allianz.t2i.client.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.contracts.types.NostroTransaction
import com.allianz.t2i.common.workflows.flows.AbstractNotifyNostroTransaction
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