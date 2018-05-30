package com.r3.cash.issuer.common.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
abstract class AbstractUpdateBankAccountRecord : FlowLogic<SignedTransaction>()