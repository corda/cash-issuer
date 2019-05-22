package com.r3.corda.finance.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.token.money.FiatCurrency
import com.r3.corda.sdk.token.workflow.flows.redeem.RedeemToken
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
@StartableByService
class RedeemCash(val amount: Amount<FiatCurrency>, val issuer: Party) : FlowLogic<SignedTransaction>() {

    companion object {
        object REDEEMING : ProgressTracker.Step("Redeeming cash.")
        @JvmStatic
        fun tracker() = ProgressTracker(REDEEMING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val tokenType = amount.token
        return subFlow(RedeemToken.InitiateRedeem(tokenType, issuer, amount, anonymous = true))
    }
}