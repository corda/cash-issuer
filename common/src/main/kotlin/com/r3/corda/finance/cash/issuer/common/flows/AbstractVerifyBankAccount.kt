package com.r3.corda.finance.cash.issuer.common.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
abstract class AbstractVerifyBankAccount : FlowLogic<SignedTransaction>() {

}