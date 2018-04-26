package com.r3.corda.finance.cash.issuer.common.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

@InitiatingFlow
abstract class AbstractRedeemCash : FlowLogic<Unit>()