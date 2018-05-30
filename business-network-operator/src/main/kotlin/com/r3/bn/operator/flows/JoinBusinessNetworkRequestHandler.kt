package com.r3.bn.operator.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.bn.common.flows.AbstractJoinBusinessNetworkRequest
import com.r3.bn.operator.hasMembershipRecord
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(AbstractJoinBusinessNetworkRequest::class)
class JoinBusinessNetworkRequestHandler(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Validate the join request before signing it.
        val signedTransaction = subFlow(object : SignTransactionFlow(otherSession) {
            // TODO: Implement checks.
            override fun checkTransaction(stx: SignedTransaction) {
                // Bail if the counterparty already has a membership record.
                val maybeMember = otherSession.counterparty.name
                if (hasMembershipRecord(maybeMember, serviceHub)) {
                    throw FlowException("$maybeMember already has a membership record.")
                }
            }
        })

        // For now, we are sending back a "Unit" as a placeholder for the business network operators CorDapp JAR.
        // TODO: Replace this with the actual CorDapp JAR.
        otherSession.send(Unit)

        // Sleep this flow until the join request state has been committed to the ledger.
        return waitForLedgerCommit(signedTransaction.id)
    }

}
