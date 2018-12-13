package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.contracts.NodeTransactionContract
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.states.NostroTransactionState
import com.r3.corda.finance.cash.issuer.common.types.NodeTransactionStatus
import com.r3.corda.finance.cash.issuer.common.utilities.getPendingRedemptionByNotes
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByService
class ProcessRedemptionPayment(val signedTransaction: SignedTransaction) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val counterparty = signedTransaction.tx.toLedgerTransaction(serviceHub).referenceInputRefsOfType<BankAccountState>().single {
            it.state.data.owner != ourIdentity
        }.state.data.owner
        logger.info("Counterparty to redeem to is $counterparty")
        val notes = signedTransaction.tx.outputsOfType<NostroTransactionState>().single().description
        val pendingRedemption = try {
            getPendingRedemptionByNotes(notes, serviceHub)!!
        } catch (e: Throwable) {
            throw IllegalStateException("ERROR!!! Oh no!!! The issuer has made an erroneous redemption payment!")
        }
        val totalRedemptionAmountPending = pendingRedemption.state.data.amountTransfer.quantityDelta
        val transactionAmount = signedTransaction.tx.outputsOfType<NostroTransactionState>().single().amountTransfer.quantityDelta
        logger.info("Total redemption amount pending is $totalRedemptionAmountPending. Tx amount is $transactionAmount")
        logger.info("Total redemption payments")
        require(totalRedemptionAmountPending == transactionAmount) { "The payment must equal the redemption amount requested." }
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary = notary)
        transactionBuilder
                .addInputState(pendingRedemption)
                .addOutputState(pendingRedemption.state.data.copy(status = NodeTransactionStatus.COMPLETE), NodeTransactionContract.CONTRACT_ID)
                .addReferenceState(signedTransaction.tx.outRefsOfType<NostroTransactionState>().single().referenced())
                .addCommand(NodeTransactionContract.Update(), listOf(ourIdentity.owningKey))
        val stx = serviceHub.signInitialTransaction(transactionBuilder)
        subFlow(FinalityFlow(stx, emptySet<FlowSession>()))
        logger.info(stx.tx.toString())
    }
}