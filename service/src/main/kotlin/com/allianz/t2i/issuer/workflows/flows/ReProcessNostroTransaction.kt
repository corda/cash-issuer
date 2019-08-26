package com.allianz.t2i.issuer.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.issuer.services.UpdateObserverService
import com.allianz.t2i.common.contracts.states.BankAccountState
import com.allianz.t2i.common.workflows.utilities.getNostroTransactionsByAccountNumber
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByService

/**
 * A flow to decide which nostro transactions to re-process when a new bank account state is received. This workflow
 * cannot be performed in the [UpdateObserverService] which seems to be the natural place for it... This is because
 * the [UpdateObserverService] observes updates from the vault on the [Schedulers.io] thread pool which doesn't have
 * access to ThreadLocal<CordaPersistence>.
 */
@StartableByService
@InitiatingFlow
class ReProcessNostroTransaction(val bankAccountState: BankAccountState) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        // TODO: this seems to have a bug. New bank account states are added to all nostro txs with missing accounts.
        // The assumption is that we only pull out transactions which have been partially matched. I.e. matched to the
        // issuer's nostro account only.
        val matchedTransactions = getNostroTransactionsByAccountNumber(bankAccountState.accountNumber, serviceHub)

        if (matchedTransactions.isEmpty()) {
            UpdateObserverService.logger.info("No transactions with this newly added account have been seen before.")
            return
        }

        UpdateObserverService.logger.info("There are ${matchedTransactions.size} which need re-processing.")

        // Process, again, all the nostro transactions with this bank account.
        matchedTransactions.forEach { subFlow(ProcessNostroTransaction(it)) }
        UpdateObserverService.logger.info("Reprocessed ${matchedTransactions.size} nostro transactions.")
    }
}