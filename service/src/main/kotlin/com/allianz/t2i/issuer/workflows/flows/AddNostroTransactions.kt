package com.allianz.t2i.issuer.workflows.flows

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.contracts.NostroTransactionContract
import com.allianz.t2i.common.contracts.states.NostroTransactionState
import com.allianz.t2i.common.contracts.types.NostroTransaction
import com.allianz.t2i.common.contracts.types.toState
import com.allianz.t2i.common.workflows.utilities.getNostroTransactionStateByTransactionId
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.Instant

// TODO: Change this to add one transaction at a time.
// Don't pass back the timestamp of the last added transaction. as this flow could be used to add old transactions.

/**
 * Records a list of nostro transaction objects. We can batch up issuance of these as nostro transaction states always
 * remain private to the issuer node. We want this flow to finish as soon as possible, so we can return back to the
 * daemon process with the transactions which have been committed up to this point.
 */
@StartableByRPC
@InitiatingFlow
class AddNostroTransactions(val newNostroTransactions: List<NostroTransaction>) : FlowLogic<Map<String, Instant>>() {

    companion object {
        // TODO: Add the rest of the progress tracker.
        object FINALISING : ProgressTracker.Step("Finalising transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        @JvmStatic
        fun tracker() = ProgressTracker(FINALISING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): Map<String, Instant> {
        // As we are polling for new transactions, there might not be any new transactions to add.
        // This should really be checked on the RPC client side but just double checking it here
        // as well, otherwise we'll end up trying to commit transactions with no output states!
        logger.info("Starting AddNostroTransaction flow...")

        newNostroTransactions.forEach { logger.info(it.toString()) }

        if (newNostroTransactions.isEmpty()) {
            return emptyMap()
        }

        // Filter out transactions which have been added before.
        val transactionsToRecord = newNostroTransactions.filter { (transactionId) ->
            getNostroTransactionStateByTransactionId(transactionId, serviceHub) == null
        }

        if (transactionsToRecord.isEmpty()) {
            return emptyMap()
        }

        // Convert into an unmapped nostro transaction state.
        val nostroTransactionStates = transactionsToRecord.map { it.toState(ourIdentity) }

        // Now, commit the nostro transaction records to the ledger. It's only the issuer that sees this though.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // It's always the issuer that signs.
        val command = Command(NostroTransactionContract.Add(), listOf(ourIdentity.owningKey))

        // Create a transaction builder, then add all the nostro transaction states and command.
        val unsignedTransaction = TransactionBuilder(notary = notary).addCommand(command)
        nostroTransactionStates.forEach {
            unsignedTransaction.addOutputState(it, NostroTransactionContract.CONTRACT_ID)
        }

        val signedTransaction = serviceHub.signInitialTransaction(unsignedTransaction)
        progressTracker.currentStep = FINALISING
        val finalisedTransaction = subFlow(FinalityFlow(signedTransaction, emptySet<FlowSession>(), Companion.FINALISING.childProgressTracker()))

        // The flow returns the IDs and timestamps of the last updates for each nostro account so
        // the daemon knows what has been recorded to date.
        val outputs = finalisedTransaction.tx.outRefsOfType<NostroTransactionState>().map { it.state.data }
        val outputsByAccountId = outputs.groupBy({ it.accountId }, { it.createdAt })
        return outputsByAccountId.mapValues { it.value.max()!! } // Map values will never be empty lists.
    }

}