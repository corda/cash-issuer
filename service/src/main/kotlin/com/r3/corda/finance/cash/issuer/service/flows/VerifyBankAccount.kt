package com.r3.corda.finance.cash.issuer.service.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.contracts.BankAccountContract
import com.r3.corda.finance.cash.issuer.common.types.AccountNumber
import com.r3.corda.finance.cash.issuer.common.utilities.getBankAccountStateByAccountNumber
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * For now accounts must be looked-up via account number.
 * TODO: Add constructors for other lookup options.
 */
@StartableByService
@StartableByRPC
class VerifyBankAccount(val accountNumber: AccountNumber) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Starting VerifyBankAccount flow.")
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val bankAccountStateAndRef = getBankAccountStateByAccountNumber(accountNumber, serviceHub)
                ?: throw FlowException("Bank account $accountNumber not found.")

        val bankAccountState = bankAccountStateAndRef.state.data
        if (bankAccountState.verified) {
            throw FlowException("Bank account $accountNumber is already verified.")
        }

        logger.info("Updating verified flag for ${bankAccountState.accountNumber}.")
        val updatedBankAccountState = bankAccountState.copy(verified = true)
        val command = Command(BankAccountContract.Update(), listOf(ourIdentity.owningKey))
        val utx = TransactionBuilder(notary = notary)
                .addInputState(bankAccountStateAndRef)
                .addCommand(command)
                .addOutputState(updatedBankAccountState, BankAccountContract.CONTRACT_ID)
        val stx = serviceHub.signInitialTransaction(utx)
        // Share the updated bank account state with the owner.
        return subFlow(FinalityFlow(stx, setOf(bankAccountState.owner)))
    }

}
