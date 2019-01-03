package com.r3.corda.finance.cash.issuer.common.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

object VerifyBankAccountFlow {

    @InitiatingFlow
    abstract class AbstractVerifyBankAccount : FlowLogic<SignedTransaction>()

    @InitiatedBy(AbstractVerifyBankAccount::class)
    class ReceiveVerifyBankAccount(val otherSession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            logger.info("Starting ReceiveVerifyBankAccount flow...")
            //return subFlow(ReceiveTransactionFlow(otherSession, true, StatesToRecord.ALL_VISIBLE))
            if (!serviceHub.myInfo.isLegalIdentity(otherSession.counterparty)) {
                subFlow(ReceiveFinalityFlow(otherSession))
            }
        }
    }
}