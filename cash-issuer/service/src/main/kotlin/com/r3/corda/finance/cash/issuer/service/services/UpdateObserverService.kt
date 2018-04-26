package com.r3.corda.finance.cash.issuer.service.services

import com.r3.corda.finance.cash.issuer.common.contracts.BankAccountContract
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.states.NostroTransactionState
import com.r3.corda.finance.cash.issuer.common.types.NostroTransactionStatus
import com.r3.corda.finance.cash.issuer.common.types.NostroTransactionType
import com.r3.corda.finance.cash.issuer.service.contracts.NostroTransactionContract
import com.r3.corda.finance.cash.issuer.service.flows.*
import net.corda.core.contracts.CommandData
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor
import rx.schedulers.Schedulers

@CordaService
class UpdateObserverService(val services: AppServiceHub) : SingletonSerializeAsToken() {

    companion object {
        val logger = loggerFor<UpdateObserverService>()
    }

    init {
        // All this processing is punted off to a thread pool.
        // Always watch for new transactions and updates. Caution, if the node dies between an
        // event being emitted and the flow starting, then the flows will have to be started manually.
        // We can do this by pulling out all the nostro transaction states and running the process flow over
        // all UNMATCHED states.
        services.validatedTransactions.updates.observeOn(Schedulers.io()).subscribe({ signedTransaction ->

            val isAddBankAccount = checkCommand<BankAccountContract.Add>(signedTransaction)
            val isUpdateBankAccount = checkCommand<BankAccountContract.Update>(signedTransaction)
            val isAddNostroTransaction = checkCommand<NostroTransactionContract.Add>(signedTransaction)
            val isMatchNostroTransaction = checkCommand<NostroTransactionContract.Match>(signedTransaction)

            logger.info("isAddBankAccount=$isAddBankAccount,isAddNostroTx=$isAddNostroTransaction,isMatchNostroTx=" +
                    "$isMatchNostroTransaction,isUpdateBankAccount=$isUpdateBankAccount")

            when {
                isAddBankAccount -> {
                    logger.info("New bank account added.")
                    addBankAccountAction(signedTransaction)
                }
                isUpdateBankAccount -> {
                    logger.info("Just updated a BankAccountState. Don't need to do anything.")
                }
                isAddNostroTransaction -> {
                    logger.info("Processing a newly added nostro transaction...")
                    addNostroTransactionAction(signedTransaction)
                }
                isMatchNostroTransaction -> {
                    logger.info("A full or partial nostro transaction match has occurred...")
                    matchNostroTransactionAction(signedTransaction)
                }
                else -> logger.info("Transaction type not recognised.")
            }
        }, { throwable -> logger.info(throwable.message) })
    }

    inline fun <reified T : CommandData> checkCommand(stx: SignedTransaction): Boolean {
        return stx.tx.commands.singleOrNull { it.value is T } != null
    }

    /**
     * We are the issuer, all our bank accounts should be verified - they are ours!
     */
    private fun addBankAccountAction(signedTransaction: SignedTransaction) {
        val bankAccountState = signedTransaction.tx.outputStates.single() as BankAccountState
        if (bankAccountState.owner == services.myInfo.legalIdentities.single()) {
            logger.info("The issuer has just added an account. It can be immediately verified.")
            val transaction = signedTransaction.tx
            val accountNumber = transaction.outRefsOfType<BankAccountState>().single().state.data.accountNumber
            services.startFlow(VerifyBankAccount(accountNumber))
            // TODO We probably need to reprocess here as well...
        } else {
            logger.info("We've received an account from another node.")
            services.startFlow(ReProcessNostroTransaction(bankAccountState))
        }
    }

    private fun addNostroTransactionAction(signedTransaction: SignedTransaction) {
        val transaction = signedTransaction.tx
        // Process all nostro transactions which have been added.
        // We can add more than one at a time.
        transaction.outRefsOfType<NostroTransactionState>().forEach {
            services.startFlow(ProcessNostroTransaction(it))
        }
    }

    private fun matchNostroTransactionAction(signedTransaction: SignedTransaction) {
        val transaction = signedTransaction.tx
        // Get the nostro transaction and check if it has been matched.
        val nostroTransactionState = transaction.outputsOfType<NostroTransactionState>().single()
        // Check whether the conditions for issuance are satisfied.
        val isMatched = nostroTransactionState.status == NostroTransactionStatus.MATCHED
        val isIssuance = nostroTransactionState.type == NostroTransactionType.ISSUANCE
        val isRedemption = nostroTransactionState.type == NostroTransactionType.REDEMPTION
        logger.info("isMatched=$isMatched,isIssuance=$isIssuance,isRedemption=$isRedemption")
        // Start the issue cash flow.
        when {
            isMatched && isIssuance -> {
                logger.info("Issuing cash!")
                services.startFlow(IssueCash(signedTransaction))
            }
            isMatched && isRedemption -> {
                logger.info("Processing redemption payment...")
                services.startFlow(ProcessRedemptionPayment(signedTransaction))
            }
        }
    }

}