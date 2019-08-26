package com.allianz.t2i.issuer.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.contracts.BankAccountContract
import com.allianz.t2i.common.workflows.flows.AbstractVerifyBankAccount
import com.allianz.t2i.common.workflows.utilities.getBankAccountStateByLinearId
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * For now accounts must be looked-up via account number.
 * TODO: Add constructors for other lookup options.
 */
@StartableByService
@StartableByRPC
class VerifyBankAccount(
        val linearId: UniqueIdentifier
) : AbstractVerifyBankAccount() {
    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Starting VerifyBankAccount flow.")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val bankAccountStateAndRef = getBankAccountStateByLinearId(linearId, serviceHub)
                ?: throw FlowException("Bank account with linearId $linearId not found.")

        val bankAccountState = bankAccountStateAndRef.state.data
        if (bankAccountState.verified) {
            throw FlowException("Bank account ${bankAccountState.accountNumber} is already verified.")
        }

        val ownerSsession = initiateFlow(bankAccountState.owner)

        logger.info("Updating verified flag for ${bankAccountState.accountNumber}.")
        val updatedBankAccountState = bankAccountState.copy(verified = true)
        val command = Command(BankAccountContract.Update(), listOf(ourIdentity.owningKey))
        val utx = TransactionBuilder(notary = notary).apply {
            addInputState(bankAccountStateAndRef)
            addCommand(command)
            addOutputState(updatedBankAccountState)
        }

        // Sign with legal identity key.
        val stx = serviceHub.signInitialTransaction(utx)
        val sessions = if (serviceHub.myInfo.isLegalIdentity(bankAccountState.owner)) emptyList() else listOf(ownerSsession)
        return subFlow(FinalityFlow(stx, sessions))
    }
}
