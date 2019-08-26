package com.allianz.t2i.common.workflows.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

@InitiatingFlow
abstract class AbstractNotifyNostroTransaction : FlowLogic<Unit>()