package com.r3.corda.finance.cash.issuer.service.services

import com.r3.corda.finance.cash.issuer.common.contracts.BankAccountContract
import com.r3.corda.finance.cash.issuer.common.states.NostroTransactionState
import com.r3.corda.finance.cash.issuer.common.types.NostroTransactionStatus
import com.r3.corda.finance.cash.issuer.common.types.NostroTransactionType
import com.r3.corda.finance.cash.issuer.service.contracts.NostroTransactionContract
import com.r3.corda.finance.cash.issuer.service.flows.ProcessNostroTransaction
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor
import rx.schedulers.Schedulers

@CordaService
class NewNostroTransactionObserver(val services: AppServiceHub) : SingletonSerializeAsToken() {

    companion object {
        val logger = loggerFor<NewNostroTransactionObserver>()
    }

    init {
        // All this processing is punted off to a thread pool.
        // Always watch for new transactions and updates. Caution, if the node dies between an
        // event being emitted and the flow starting, then the flows will have to be started manually.
        // We can do this by pulling out all the nostro transaction states and running the process flow over
        // all UNMATCHED states.
        services.validatedTransactions.updates.observeOn(Schedulers.io()).subscribe { signedTransaction ->
            when {
                isAddBankAccount(signedTransaction) -> {
                    logger.info("Start the verify flow. Issuer can auto verify their own accounts!")
                }
                isAddNostroTransaction(signedTransaction) -> {
                    logger.info("Processing a newly added nostro transaction...")
                    addNostroTransactionAction(signedTransaction)
                }
                isMatchNostroTransaction(signedTransaction) -> {
                    logger.info("A full or partial nostro transaction match has occured...")
                    matchNostroTransactionAction(signedTransaction)
                }
            }
        }
    }

    private fun isAddBankAccount(stx: SignedTransaction) = stx.tx.commands.singleOrNull {
        it.value is BankAccountContract.Add
    } != null

    private fun isAddNostroTransaction(stx: SignedTransaction) = stx.tx.commands.singleOrNull {
        it.value is NostroTransactionContract.Add
    } != null

    private fun isMatchNostroTransaction(stx: SignedTransaction) = stx.tx.commands.singleOrNull {
        it.value is NostroTransactionContract.Match
    } != null

    private fun addNostroTransactionAction(signedTransaction: SignedTransaction) {
        val transaction = signedTransaction.tx
        // Process all nostro transactions which have been added.
        // We can add more than one at a time.
        transaction.outRefsOfType<NostroTransactionState>().forEach {
            logger.info("Adding a state")
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

        // Start the issue cash flow.
        if (isMatched && isIssuance) {
            services.startFlow(IssueCash(signedTransaction))
        }
    }

}