package com.allianz.t2i.common.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.move.ConfidentialMoveFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

/**
 * Simple move cash flow for demos.
 */
@InitiatingFlow
class MoveCash(val recipient: Party, val amount: Amount<TokenType>) : FlowLogic<SignedTransaction>() {

    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val recipientSession = initiateFlow(recipient)
        val changeKey = serviceHub.keyManagementService.freshKeyAndCert(ourIdentityAndCert, false).party.anonymise()
        return subFlow(ConfidentialMoveFungibleTokensFlow(
                partiesAndAmounts = listOf(PartyAndAmount(recipient, amount)),
                participantSessions = listOf(recipientSession),
                observerSessions = emptyList(),
                queryCriteria = null,
                changeHolder = changeKey
        ))
    }
}

@StartableByRPC
class MoveCashShell(val recipient: Party, val amount: Long, val currency: String) : FlowLogic<SignedTransaction>() {

    companion object {
        object MOVING : ProgressTracker.Step("Moving cash.")

        @JvmStatic
        fun tracker() = ProgressTracker(MOVING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val fiatCurrency = FiatCurrency.getInstance(currency)
        return subFlow(MoveCash(recipient, amount of fiatCurrency))
    }
}