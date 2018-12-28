package com.r3.corda.finance.cash.issuer.common.flows

import net.corda.core.flows.InitiatingFlow
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.flows.AbstractCashFlow

@InitiatingFlow
abstract class AbstractIssueCash<out T>(override val progressTracker: ProgressTracker) : AbstractCashFlow<T>(progressTracker)