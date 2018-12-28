package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.contracts.NodeTransactionContract
import com.r3.corda.finance.cash.issuer.common.states.NodeTransactionState
import com.r3.corda.finance.cash.issuer.common.types.NodeTransactionStatus
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.flows.AbstractCashFlow
import net.corda.finance.flows.CashIssueAndPaymentFlow
import net.corda.finance.flows.CashIssueFlow


// TODO: Denominations? E.g. 100 split between 10 issuances of 10.
// TODO: Ask the counterparty for a random key and issue to that key.
// TODO: Move all tx generation stuff to the common library.
// TODO: Add the option for issuing to anonymous public keys.

@InitiatingFlow
@StartableByService
class IssueCash(val stx: SignedTransaction) : FlowLogic<Pair<SignedTransaction, SignedTransaction>>() {
    companion object {
        object GENERATING_TX : ProgressTracker.Step("Generating node transaction")
        object SIGNING_TX : ProgressTracker.Step("Signing node transaction")
        object FINALISING_TX : ProgressTracker.Step("Obtaining notary signature and recording node transaction") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }
        object ISSUE_CASH : ProgressTracker.Step("Issue cash internal") {
            override fun childProgressTracker() = AbstractCashFlow.tracker()
        }


        @JvmStatic
        fun tracker() = ProgressTracker(
                GENERATING_TX,
                SIGNING_TX,
                FINALISING_TX,
                ISSUE_CASH
        )
    }

    override val progressTracker = tracker()
    @Suspendable
    override fun call(): Pair<SignedTransaction, SignedTransaction> {
        // Our chosen notary.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val nodeTransactionStateAndRef = stx.tx.outRefsOfType<NodeTransactionState>().single()
        val nodeTransactionState = nodeTransactionStateAndRef.state.data

        progressTracker.currentStep = GENERATING_TX
        // TODO: Check that the bank account states are verified.
        val internalBuilder = TransactionBuilder(notary = notary)
                .addCommand(NodeTransactionContract.Update(), listOf(ourIdentity.owningKey))
                .addInputState(nodeTransactionStateAndRef)
                .addOutputState(nodeTransactionState.copy(status = NodeTransactionStatus.COMPLETE), NodeTransactionContract.CONTRACT_ID)

        progressTracker.currentStep = SIGNING_TX
        val signedTransaction = serviceHub.signInitialTransaction(internalBuilder)

        progressTracker.currentStep = FINALISING_TX
        val internalFtx = subFlow(FinalityFlow(signedTransaction, emptySet<FlowSession>(), FINALISING_TX.childProgressTracker()))

        /** Commit the cash issuance transaction. */
        progressTracker.currentStep = ISSUE_CASH
        val recipient = nodeTransactionState.amountTransfer.destination
        val amount = Amount(nodeTransactionState.amountTransfer.quantityDelta, nodeTransactionState.amountTransfer.token)
        //val externalFtx = subFlow(CashIssueAndPaymentFlow(amount, OpaqueBytes.of(0), recipient, false, notary))
        val externalRes = subFlow(IssueCashInternal(recipient, amount, ISSUE_CASH.childProgressTracker()))

        return Pair(internalFtx, externalRes.stx)
    }

}