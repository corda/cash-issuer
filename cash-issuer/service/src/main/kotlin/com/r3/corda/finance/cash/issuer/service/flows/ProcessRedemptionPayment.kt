package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.utilities.getPendingRedemptionsByCounterparty
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.SignedTransaction

class ProcessRedemptionPayment(val signedTransaction: SignedTransaction) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // 1. Determine who the counter-party is.
        val counterparty = signedTransaction.tx.outRefsOfType<BankAccountState>().single {
            it.state.data.owner != ourIdentity
        }.state.data.owner
        // 2. Gather all of the Redemption PENDING NodeTransactionStates with a destination equal to the counterparty.
        val pendingRedemptions = getPendingRedemptionsByCounterparty(counterparty.toString(), serviceHub)
                ?: throw IllegalStateException("ERROR!!! Oh no!!! The issuer has made an erroneous redemption payment!")
        val totalRedemptionAmountPending = pendingRedemptions.map {
            it.state.data.amountTransfer.quantityDelta
        }.sum()
        logger.info("Total redemption payments")
    }
}