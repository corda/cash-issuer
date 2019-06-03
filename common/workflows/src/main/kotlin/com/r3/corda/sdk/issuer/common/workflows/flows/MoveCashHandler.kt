package com.r3.corda.sdk.issuer.common.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.token.workflow.flows.move.ConfidentialMoveTokensFlowHandler
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy

/**
 * Simple move cash flow for demos.
 */
@InitiatedBy(MoveCash::class)
class MoveCashHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ConfidentialMoveTokensFlowHandler(otherSession))
    }
}