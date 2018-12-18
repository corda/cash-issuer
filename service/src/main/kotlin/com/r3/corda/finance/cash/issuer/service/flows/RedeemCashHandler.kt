package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.contracts.NodeTransactionContract
import com.r3.corda.finance.cash.issuer.common.flows.AbstractRedeemCash
import com.r3.corda.finance.cash.issuer.common.states.NodeTransactionState
import com.r3.corda.finance.cash.issuer.common.types.NodeTransactionType
import com.r3.corda.finance.cash.issuer.common.utilities.GenerationScheme
import com.r3.corda.finance.cash.issuer.common.utilities.generateRandomString
import net.corda.core.contracts.Amount
import net.corda.core.contracts.AmountTransfer
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.Issued
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashException
import net.corda.finance.utils.sumCash
import java.time.Instant
import java.util.*

@InitiatedBy(AbstractRedeemCash::class)
class RedeemCashHandler(val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Starting redeem handler flow.")
        val cashStateAndRefsToRedeem = subFlow(ReceiveStateAndRefFlow<Cash.State>(otherSession))
        val redemptionAmount = otherSession.receive<Amount<Issued<Currency>>>().unwrap { it }
        logger.info("Received cash states to redeem.")
        logger.info("redemptionAmount: $redemptionAmount")
        // Add create a node transaction state by adding the linearIds of all teh bank account states
        val amount = cashStateAndRefsToRedeem.map { it.state.data }.sumCash()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val transactionBuilder = TransactionBuilder(notary = notary)
        val signers = try {
            Cash().generateExit(
                    tx = transactionBuilder,
                    amountIssued = redemptionAmount,
                    assetStates = cashStateAndRefsToRedeem,
                    payChangeTo = otherSession.counterparty
            )
        } catch (e: InsufficientBalanceException) {
            throw CashException("Exiting more cash than exists", e)
        }
        val nodeTransactionState = NodeTransactionState(
                amountTransfer = AmountTransfer(
                        quantityDelta = -redemptionAmount.quantity,
                        token = amount.token.product,
                        source = ourIdentity,
                        destination = otherSession.counterparty
                ),
                notes = generateRandomString(10, GenerationScheme.NUMBERS),
                createdAt = Instant.now(),
                participants = listOf(ourIdentity),
                type = NodeTransactionType.REDEMPTION
        )
        val ledgerTx = transactionBuilder.toLedgerTransaction(serviceHub)
        ledgerTx.inputStates.forEach { logger.info((it as Cash.State).toString()) }
        transactionBuilder.addOutputState(nodeTransactionState, NodeTransactionContract.CONTRACT_ID)
        logger.info(transactionBuilder.toWireTransaction(serviceHub).toString())
        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder, serviceHub.keyManagementService.filterMyKeys(signers))
        val signedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTransaction, listOf(otherSession)))
        return subFlow(FinalityFlow(signedTransaction, listOf(otherSession)))
    }
}