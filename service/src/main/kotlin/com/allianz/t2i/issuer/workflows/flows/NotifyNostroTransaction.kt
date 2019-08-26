package com.allianz.t2i.issuer.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.contracts.types.NostroTransaction
import com.allianz.t2i.common.workflows.flows.AbstractNotifyNostroTransaction
import net.corda.core.identity.Party

class NotifyNostroTransaction(
        val nostroTransaction: NostroTransaction,
        val counterparty: Party
) : AbstractNotifyNostroTransaction() {
    @Suspendable
    override fun call() = initiateFlow(counterparty).send(nostroTransaction)
}