package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.contracts.asset.Cash
import java.util.*

class IssueCashInternal(val to: AbstractParty, val amount: Amount<Currency>) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(Cash.Commands.Issue(), listOf(ourIdentity.owningKey))
        // Create a new cash state.
        // The issuer reference is the hash of the transaction containing the nostro transaction state which led to
        // this issuance. We include this so we can check it during redemption time. Using this as the reference means
        // that cash states won't get merged very often as it's likely that the reference is different (one for each
        // issuance).
        // TODO: Issuer references is a dud concept. We should deprecate it in Corda Core.
        val partyAndReference = PartyAndReference(ourIdentity, OpaqueBytes.of(0))
        val issuerAndToken = Issued(partyAndReference, amount.token)
        val issuedAmount = Amount(amount.quantity, issuerAndToken)
        val cashState = Cash.State(
                amount = issuedAmount,
                owner = to
        )

        val externalBuilder = TransactionBuilder(notary = notary)
                .addOutputState(cashState, Cash.PROGRAM_ID)
                .addCommand(command)

        // Sign and finalise.
        val externalStx = serviceHub.signInitialTransaction(externalBuilder)
        return subFlow(FinalityFlow(externalStx, emptySet<FlowSession>()))
    }
}