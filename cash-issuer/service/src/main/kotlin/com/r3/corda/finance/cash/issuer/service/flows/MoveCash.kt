package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.asset.PartyAndAmount
import java.util.*

// This is just temporary for demoing.
@StartableByRPC
class MoveCash(val recipients: List<PartyAndAmount<Currency>>, val anonymous: Boolean) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary = notary)
        Cash.generateSpend(services = serviceHub, tx = transactionBuilder, payments = recipients, ourIdentity = ourIdentityAndCert)
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder)
        return subFlow(FinalityFlow(signedTransaction))
    }
}