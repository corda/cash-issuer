package com.r3.corda.sdk.issuer.common.workflows.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

@InitiatingFlow
abstract class AbstractRedeemCash : FlowLogic<Unit>()