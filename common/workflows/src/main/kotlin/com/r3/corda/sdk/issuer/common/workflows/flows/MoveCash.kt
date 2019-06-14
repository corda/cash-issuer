package com.r3.corda.sdk.issuer.common.workflows.flows

import co.paralleluniverse.fibers.Suspendable
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
@StartableByRPC
@InitiatingFlow
class MoveCash(val recipient: Party, val amount: Amount<FiatCurrency>) : FlowLogic<SignedTransaction>() {

    companion object {
        // TODO: Add the rest of the progress tracker.
        object MOVING : ProgressTracker.Step("Moving cash.")

        @JvmStatic
        fun tracker() = ProgressTracker(MOVING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val recipientSession = initiateFlow(recipient)
        return subFlow(ConfidentialMoveFungibleTokensFlow(
                partiesAndAmounts = listOf(PartyAndAmount(recipient, amount)),
                participantSessions = listOf(recipientSession),
                observerSessions = emptyList(),
                queryCriteria = null,
                changeHolder = null
        ))
    }
}