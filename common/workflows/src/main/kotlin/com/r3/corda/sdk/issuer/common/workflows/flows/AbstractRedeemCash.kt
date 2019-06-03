package com.r3.corda.sdk.issuer.common.workflows.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
abstract class AbstractRedeemCash : FlowLogic<SignedTransaction>()