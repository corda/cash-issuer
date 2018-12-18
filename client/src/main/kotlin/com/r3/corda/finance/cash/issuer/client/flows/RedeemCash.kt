package com.r3.corda.finance.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.flows.AbstractRedeemCash
import net.corda.core.contracts.Amount
import net.corda.core.contracts.PartyAndReference
import net.corda.core.flows.*
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
        object REDEEMING : ProgressTracker.Step("Redeeming cash.")
        @JvmStatic
        fun tracker() = ProgressTracker(REDEEMING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call() {
        val builder = TransactionBuilder(notary = null)
        //get unconsumed cash states
        val exitStates = AbstractCashSelection
                .getInstance { serviceHub.jdbcSession().metaData }
                .unconsumedCashStatesForSpending(serviceHub, amount, setOf(issuer), builder.notary, builder.lockId, setOf())
        exitStates.forEach { logger.info(it.state.data.toString()) }

        progressTracker.currentStep = REDEEMING
        val otherSession = initiateFlow(issuer)
        logger.info("Sending states to exit to $issuer")

        //sign tx
        subFlow(SendStateAndRefFlow(otherSession, exitStates))

        otherSession.send(amount.issuedBy(PartyAndReference(issuer, OpaqueBytes.of(0))))

        subFlow(object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) = Unit
        })

        subFlow(ReceiveFinalityFlow(otherSession))
    }
}