package com.r3.corda.finance.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.issuer.common.workflows.flows.AbstractRedeemCash
import com.r3.corda.sdk.token.money.FiatCurrency
import com.r3.corda.sdk.token.workflow.flows.redeem.ConfidentialRedeemFungibleTokensFlow
import net.corda.core.contracts.Amount
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
@StartableByService
class RedeemCash(val amount: Amount<FiatCurrency>, val issuer: Party) : AbstractRedeemCash() {

    companion object {
        object REDEEMING : ProgressTracker.Step("Redeeming cash.")
        @JvmStatic
        fun tracker() = ProgressTracker(REDEEMING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val issuerSession = initiateFlow(issuer)
        return subFlow(ConfidentialRedeemFungibleTokensFlow(amount, issuer, issuerSession))
    }
}