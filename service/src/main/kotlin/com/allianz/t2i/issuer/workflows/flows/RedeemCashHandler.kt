package com.allianz.t2i.issuer.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.flows.redeem.ConfidentialRedeemFungibleTokensFlowHandler
import com.allianz.t2i.common.workflows.flows.AbstractRedeemCash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy

// TODO: Need to refactor this using Kasia's updated Redeem flow.

@InitiatedBy(AbstractRedeemCash::class)
class RedeemCashHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        logger.info("Starting redeem handler flow.")
        // We probably shouldn't have a type parameter on the responder.
        subFlow(ConfidentialRedeemFungibleTokensFlowHandler(otherSession))
    }
}