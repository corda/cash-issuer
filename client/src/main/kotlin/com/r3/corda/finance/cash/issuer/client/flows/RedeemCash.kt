package com.r3.corda.finance.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.flows.AbstractRedeemCash
import net.corda.core.contracts.Amount
import net.corda.core.contracts.PartyAndReference
import net.corda.core.flows.SendStateAndRefFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.cash.selection.AbstractCashSelection
import net.corda.finance.issuedBy
import java.util.*

@StartableByRPC
@StartableByService
class RedeemCash(val amount: Amount<Currency>, val issuer: Party) : AbstractRedeemCash() {

    companion object {
        // TODO: Add the rest of the progress tracker.
        object REDEEMING : ProgressTracker.Step("Redeeming cash.")

        fun tracker() = ProgressTracker(REDEEMING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call() {
        val builder = TransactionBuilder(notary = null)
        val exitStates = AbstractCashSelection
                .getInstance { serviceHub.jdbcSession().metaData }
                .unconsumedCashStatesForSpending(serviceHub, amount, setOf(issuer), builder.notary, builder.lockId, setOf())
        exitStates.forEach { logger.info(it.state.data.toString()) }
        val session = initiateFlow(issuer)
        logger.info("Sending states to exit to $issuer")
        progressTracker.currentStep = REDEEMING
        subFlow(SendStateAndRefFlow(session, exitStates))
        session.send(amount.issuedBy(PartyAndReference(issuer, OpaqueBytes.of(0))))

        subFlow(object : SignTransactionFlow(session) {
            override fun checkTransaction(stx: SignedTransaction) = Unit
        })
    }
}