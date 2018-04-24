package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.transactions.SignedTransaction

// redeem cash initiator, redeem cash handler.
class RedeemCash : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

    }
}