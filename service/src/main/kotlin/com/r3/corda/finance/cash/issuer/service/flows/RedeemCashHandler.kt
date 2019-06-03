package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.issuer.common.workflows.flows.AbstractRedeemCash
import com.r3.corda.sdk.token.money.FiatCurrency
import com.r3.corda.sdk.token.workflow.flows.redeem.ConfidentialRedeemFungibleTokensFlowHandler
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
        subFlow(ConfidentialRedeemFungibleTokensFlowHandler<FiatCurrency>(otherSession))
    }
}