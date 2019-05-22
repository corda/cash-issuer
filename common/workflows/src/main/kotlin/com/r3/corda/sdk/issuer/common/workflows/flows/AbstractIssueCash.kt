package com.r3.corda.sdk.issuer.common.workflows.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
abstract class AbstractIssueCash<out T>(override val progressTracker: ProgressTracker) : FlowLogic<T>()