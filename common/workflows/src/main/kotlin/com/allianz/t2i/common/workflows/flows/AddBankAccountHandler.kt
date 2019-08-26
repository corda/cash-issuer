package com.allianz.t2i.common.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.node.StatesToRecord

@InitiatedBy(AddBankAccount::class)
class AddBankAccountHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
            // The verifier should store the transaction using ALL VISIBLE as they are not a participant in the state.
            subFlow(ReceiveFinalityFlow(otherSession, statesToRecord = StatesToRecord.ALL_VISIBLE))
        }
    }
}