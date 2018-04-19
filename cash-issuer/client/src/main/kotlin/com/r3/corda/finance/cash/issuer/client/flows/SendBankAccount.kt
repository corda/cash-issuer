package com.r3.corda.finance.cash.issuer.client.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.flows.AbstractSendBankAccount
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.utilities.getBankAccountStateByLinearId
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.SendStateAndRefFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
@StartableByService
class SendBankAccount(val issuer: Party, val linearId: UniqueIdentifier) : AbstractSendBankAccount() {

    companion object {
        // TODO: Add the rest of the progress tracker.
        object SENDING : ProgressTracker.Step("Sending to issuer.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(SENDING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): StateAndRef<BankAccountState> {
        val bankAccountState = getBankAccountStateByLinearId(linearId, serviceHub)
                ?: throw IllegalArgumentException("LinearId $linearId does not match any bank account state.")
        val session = initiateFlow(issuer)
        progressTracker.currentStep = SENDING
        subFlow(SendStateAndRefFlow(session, listOf(bankAccountState)))
        return bankAccountState
    }

}