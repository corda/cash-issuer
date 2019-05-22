package com.r3.corda.sdk.issuer.common.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.token.money.FiatCurrency
import com.r3.corda.sdk.token.workflow.flows.move.SelectAndMoveFungibleTokensFlow
import com.r3.corda.sdk.token.workflow.types.PartyAndAmount
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

/**
 * Simple move cash flow for demos.
 */
class MoveCashHandler(val recipient: Party, val amount: Amount<FiatCurrency>) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val recipientSession = initiateFlow(recipient)
        return subFlow(SelectAndMoveFungibleTokensFlow(
                partiesAndAmounts = listOf(PartyAndAmount(recipient, amount)),
                participantSessions = listOf(recipientSession),
                observerSessions = emptyList(),
                queryCriteria = null,
                changeHolder = null
        ))
    }
}