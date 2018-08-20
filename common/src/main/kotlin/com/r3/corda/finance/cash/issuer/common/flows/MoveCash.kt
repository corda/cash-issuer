package com.r3.corda.finance.cash.issuer.common.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashException
import java.util.*

// This is just temporary for demoing.
@StartableByRPC
class MoveCash(val recipient: Party, val amount: Amount<Currency>) : FlowLogic<SignedTransaction>() {

    companion object {
        // TODO: Add the rest of the progress tracker.
        object MOVING : ProgressTracker.Step("Moving cash.")

        fun tracker() = ProgressTracker(MOVING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary = notary)
        val (_, keys) = try {
            Cash.generateSpend(services = serviceHub, tx = transactionBuilder, amount = amount, to = recipient, ourIdentity = ourIdentityAndCert)
        } catch (e: InsufficientBalanceException) {
            throw CashException("Insufficient cash for spend: ${e.message}", e)
        }
        val ledgerTx = transactionBuilder.toLedgerTransaction(serviceHub)
        ledgerTx.inputStates.forEach { logger.info((it as Cash.State).toString()) }
        logger.info(transactionBuilder.toWireTransaction(serviceHub).toString())
        val signedTransaction = serviceHub.signInitialTransaction(transactionBuilder, keys)
        progressTracker.currentStep = MOVING
        return subFlow(FinalityFlow(signedTransaction))
    }
}