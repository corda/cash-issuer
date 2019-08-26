package com.allianz.t2i.client.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.workflows.flows.AbstractIssueCash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ReceiveFinalityFlow

@InitiatedBy(AbstractIssueCash::class)
class ReceiveIssuedCash(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        logger.info("Starting ReceiveIssuedCash flow...")
        //return subFlow(ReceiveTransactionFlow(otherSession, true, StatesToRecord.ALL_VISIBLE))
        if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            subFlow(ReceiveFinalityFlow(otherSession))
        }
    }
}