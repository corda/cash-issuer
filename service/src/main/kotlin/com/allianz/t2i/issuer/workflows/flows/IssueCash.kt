package com.allianz.t2i.issuer.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.ConfidentialIssueTokens
import com.allianz.t2i.common.contracts.NodeTransactionContract
import com.allianz.t2i.common.contracts.states.NodeTransactionState
import com.allianz.t2i.common.contracts.types.NodeTransactionStatus
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

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

        @JvmStatic
        fun tracker() = ProgressTracker(GENERATING_TX, SIGNING_TX, FINALISING_TX)
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
        val internalBuilder = TransactionBuilder(notary = notary).apply {
            addCommand(NodeTransactionContract.Update(), listOf(ourIdentity.owningKey))
            addInputState(nodeTransactionStateAndRef)
            addOutputState(nodeTransactionState.copy(status = NodeTransactionStatus.COMPLETE), NodeTransactionContract.CONTRACT_ID)
        }

        progressTracker.currentStep = SIGNING_TX
        val signedTransaction = serviceHub.signInitialTransaction(internalBuilder)

        progressTracker.currentStep = FINALISING_TX
        val internalFtx = subFlow(FinalityFlow(signedTransaction, emptySet<FlowSession>(), Companion.FINALISING_TX.childProgressTracker()))

        /** Commit the cash issuance transaction. */
        val recipient = nodeTransactionState.amountTransfer.destination
        val quantity = nodeTransactionState.amountTransfer.quantityDelta
        val tokenType = nodeTransactionState.amountTransfer.token
        val tokenToIssue = quantity of tokenType issuedBy ourIdentity heldBy recipient
        val issueTx = subFlow(ConfidentialIssueTokens(tokensToIssue = listOf(tokenToIssue), observers = emptyList()))
        // Return the internal tx and the external tx.
        return Pair(internalFtx, issueTx)
    }

}