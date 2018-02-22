package com.r3.cash.issuer.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.bn.operator.getMemberByX500Name
import com.r3.bn.operator.isNotApproved
import com.r3.cash.issuer.common.flows.AbstractCreateBankAccountRecord
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(AbstractCreateBankAccountRecord::class)
class CreateBankAccountRecordHandler(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransaction = subFlow(object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                val membershipRecord = getMemberByX500Name(otherSession.counterparty.name, serviceHub)
                        ?: throw FlowException("You do not have a membership record!")
                if (membershipRecord.state.data.isNotApproved()) throw FlowException("You are not an approved member.")
            }
        })
        return waitForLedgerCommit(signedTransaction.id)
    }
}