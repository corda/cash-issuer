package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.sdk.issuer.common.contracts.NodeTransactionContract
import com.r3.corda.sdk.issuer.common.contracts.states.NodeTransactionState
import com.r3.corda.sdk.issuer.common.contracts.types.NodeTransactionType
import com.r3.corda.sdk.issuer.common.workflows.flows.AbstractRedeemCash
import com.r3.corda.sdk.issuer.common.workflows.utilities.GenerationScheme
import com.r3.corda.sdk.issuer.common.workflows.utilities.generateRandomString
import com.r3.corda.sdk.token.contracts.states.FungibleToken
import com.r3.corda.sdk.token.contracts.utilities.sumTokenStatesOrThrow
import com.r3.corda.sdk.token.contracts.utilities.sumTokenStatesOrZero
import com.r3.corda.sdk.token.money.FiatCurrency
import com.r3.corda.sdk.token.workflow.flows.redeem.RedeemToken
import net.corda.core.contracts.AmountTransfer
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

// TODO: Need to refactor this using Kasia's updated Redeem flow.

@InitiatedBy(AbstractRedeemCash::class)
class RedeemCashHandler(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Starting redeem handler flow.")
        // We probably shouldn't have a type parameter on the responder.
        val redeemTx = subFlow(RedeemToken.IssuerResponder<FiatCurrency>(otherSession))

        // Calculate the redemption amount.
        val ledgerTx = redeemTx.tx.toLedgerTransaction(serviceHub)
        val inputAmount = ledgerTx.outputsOfType<FungibleToken<FiatCurrency>>().sumTokenStatesOrThrow()
        val inputIssuedTokenType = inputAmount.token
        val outputAmount = ledgerTx.outputsOfType<FungibleToken<FiatCurrency>>().sumTokenStatesOrZero(inputIssuedTokenType)
        val redemptionAmount = inputAmount - outputAmount

        // Create the internal record. This is only used by the issuer.
        val nodeTransactionState = NodeTransactionState(
                amountTransfer = AmountTransfer(
                        quantityDelta = -redemptionAmount.quantity,
                        token = inputIssuedTokenType.tokenType,
                        source = ourIdentity,
                        destination = otherSession.counterparty
                ),
                notes = generateRandomString(10, GenerationScheme.NUMBERS),
                createdAt = Instant.now(),
                participants = listOf(ourIdentity),
                type = NodeTransactionType.REDEMPTION
        )
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val tx = TransactionBuilder(notary = notary)
        tx.addOutputState(nodeTransactionState, NodeTransactionContract.CONTRACT_ID)
        tx.addCommand(NodeTransactionContract.Create(), listOf(ourIdentity.owningKey))
        logger.info(tx.toWireTransaction(serviceHub).toString())
        val partiallySignedTransaction = serviceHub.signInitialTransaction(tx)
        val signedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(otherSession)))
        return subFlow(FinalityFlow(signedTransaction, listOf(otherSession)))
    }
}