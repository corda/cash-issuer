package com.r3.corda.finance.cash.issuer.common.flows

import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

@InitiatingFlow
abstract class AbstractSendBankAccount : FlowLogic<StateAndRef<BankAccountState>>()