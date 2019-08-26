package com.allianz.t2i.client.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.redeem.ConfidentialRedeemFungibleTokensFlow
import com.allianz.t2i.common.workflows.flows.AbstractRedeemCash
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

class RedeemCash(val amount: Amount<TokenType>, val issuer: Party) : AbstractRedeemCash() {

    companion object {
        object REDEEMING : ProgressTracker.Step("Redeeming cash.")
        @JvmStatic
        fun tracker() = ProgressTracker(REDEEMING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val issuerSession = initiateFlow(issuer)
        return subFlow(ConfidentialRedeemFungibleTokensFlow(amount, issuerSession))
    }
}

@StartableByRPC
class RedeemCashShell(val amount: Long, val currency: String, val issuer: Party) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val fiatCurrency = FiatCurrency.getInstance(currency)
        return subFlow(RedeemCash(amount of fiatCurrency, issuer))
    }
}