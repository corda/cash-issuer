package com.allianz.t2i.common.workflows.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
abstract class AbstractVerifyBankAccount : FlowLogic<SignedTransaction>()