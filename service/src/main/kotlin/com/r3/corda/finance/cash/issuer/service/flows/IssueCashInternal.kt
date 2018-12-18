package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.flows.AbstractIssueCash
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.AbstractCashFlow
import net.corda.finance.issuedBy
import java.util.*


class IssueCashInternal(val recipient: Party,
                        val amount: Amount<Currency>,
                        override val progressTracker: ProgressTracker) : AbstractIssueCash<AbstractCashFlow.Result>(progressTracker) {
    @Suspendable
    override fun call(): AbstractCashFlow.Result {
        progressTracker.currentStep = GENERATING_ID
        val recipientSession = initiateFlow(recipient)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary = notary)

        progressTracker.currentStep = GENERATING_TX
        logger.info("Generating issue for: ${builder.lockId}")
        val issuer = ourIdentity.ref(OpaqueBytes.of(0))
        val signers = Cash().generateIssue(builder, amount.issuedBy(issuer), recipient, notary)

        progressTracker.currentStep = SIGNING_TX
        logger.info("Signing transaction for: ${builder.lockId}")
        val tx = serviceHub.signInitialTransaction(builder, signers)

        progressTracker.currentStep = FINALISING_TX
        logger.info("Finalising transaction for: ${tx.id}")
        val sessionsForFinality = if (serviceHub.myInfo.isLegalIdentity(recipient)) emptyList() else listOf(recipientSession)
        val notarisedTx = finaliseTx(tx, sessionsForFinality, "Unable to notarise spend")
        logger.info("Finalised transaction for: ${notarisedTx.id}")
        return Result(notarisedTx, recipient)
    }
}