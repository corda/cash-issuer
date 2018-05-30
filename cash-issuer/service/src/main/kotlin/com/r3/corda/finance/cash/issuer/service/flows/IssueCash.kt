package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.contracts.NodeTransactionContract
import com.r3.corda.finance.cash.issuer.common.states.NodeTransactionState
import com.r3.corda.finance.cash.issuer.common.types.NodeTransactionStatus
import net.corda.core.contracts.Amount
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

// TODO: Denominations? E.g. 100 split between 10 issuances of 10.
// TODO: Ask the counterparty for a random key and issue to that key.
// TODO: Move all tx generation stuff to the common library.
// TODO: Add the option for issuing to anonymous public keys.

@StartableByService
class IssueCash(val stx: SignedTransaction) : FlowLogic<Pair<SignedTransaction, SignedTransaction>>() {

    @Suspendable
    override fun call(): Pair<SignedTransaction, SignedTransaction> {
        // Our chosen notary.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val nodeTransactionStateAndRef = stx.tx.outRefsOfType<NodeTransactionState>().single()
        val nodeTransactionState = nodeTransactionStateAndRef.state.data

        // TODO: Check that the bank account states are verified.
        val internalBuilder = TransactionBuilder(notary = notary)
                .addCommand(NodeTransactionContract.Update(), listOf(ourIdentity.owningKey))
                .addInputState(nodeTransactionStateAndRef)
                .addOutputState(nodeTransactionState.copy(status = NodeTransactionStatus.COMPLETE), NodeTransactionContract.CONTRACT_ID)

        val signedTransaction = serviceHub.signInitialTransaction(internalBuilder)
        val internalFtx = subFlow(FinalityFlow(signedTransaction))

        /** Commit the cash issuance transaction. */
        val externalFtx = subFlow(IssueCashInternal(
                to = nodeTransactionState.amountTransfer.destination,
                amount = Amount(nodeTransactionState.amountTransfer.quantityDelta, nodeTransactionState.amountTransfer.token)
        ))
        return Pair(internalFtx, externalFtx)
    }

}