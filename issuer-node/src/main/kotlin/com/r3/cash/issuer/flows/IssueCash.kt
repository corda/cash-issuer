package com.r3.cash.issuer.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByService
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.GBP
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.flows.CashPaymentFlow

@StartableByService
class IssueCash(val amount: Long) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        println("I'm issuing $amount of cash!")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val cashIssueFlow = CashIssueFlow(
                amount = Amount(amount, GBP),
                issuerBankPartyRef = OpaqueBytes.of(1),
                notary = notary
        )

        subFlow(cashIssueFlow)

        val partyA = serviceHub.networkMapCache.getNodeByLegalName(CordaX500Name("PartyA", "London", "GB"))?.legalIdentities?.first()

        val paymentRequest = CashPaymentFlow.PaymentRequest(
                amount = Amount(amount, GBP),
                recipient = partyA!!,
                anonymous = false
        )

        val cashTransferFlow = CashPaymentFlow(paymentRequest)
        return subFlow(cashTransferFlow).stx
    }

}