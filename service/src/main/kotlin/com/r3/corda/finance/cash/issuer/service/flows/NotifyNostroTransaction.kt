package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.issuer.common.contracts.types.NostroTransaction
import com.r3.corda.sdk.issuer.common.workflows.flows.AbstractNotifyNostroTransaction
import net.corda.core.identity.Party

class NotifyNostroTransaction(
        val nostroTransaction: NostroTransaction,
        val counterparty: Party
) : AbstractNotifyNostroTransaction() {
    @Suspendable
    override fun call() = initiateFlow(counterparty).send(nostroTransaction)
}