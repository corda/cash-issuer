package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.finance.cash.issuer.common.flows.AddBankAccount
import com.r3.corda.finance.cash.issuer.common.types.UKAccountNumber
import com.r3.corda.finance.cash.issuer.service.flows.GetLastUpdatesByAccountId
import com.r3.corda.finance.cash.issuer.service.flows.GetNostroAccountBalances
import net.corda.core.CordaRuntimeException
import net.corda.core.contracts.Amount
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

abstract class AbstractDaemon(val services: CordaRPCOps, val cmdLineOptions: CommandLineOptions) {
    protected val autoStart: Boolean = cmdLineOptions.autoMode

    protected val openBankingApiClients: List<OpenBankingApi> by lazy { scanForOpenBankingApiClients() }

    // Maps bank account IDs to the open banking api client which provides access.
    protected val accountsToBank: Map<BankAccountId, OpenBankingApi> by lazy {
        mutableMapOf<BankAccountId, OpenBankingApi>().apply {
            openBankingApiClients.flatMap { client ->
                client.accounts.map { account ->
                    put(account.accountId, client)
                }
            }
        }
    }

    protected var subscriber: Subscription? = null
    protected val transactionsFeed = Observable
            .interval(5, TimeUnit.SECONDS, Schedulers.io())
            .flatMap { Observable.merge(openBankingApiClients.map(OpenBankingApi::transactionsFeed)) }
            .doOnError { println(it.message) }

    init {
        // 1. Add all bank accounts for all API clients.
        addAllBankAccounts()
        // 2. Query nostro balances on Corda and via the APIs.
        val balances = getAllBalances()
        printBalances(balances)
        // 3. Get the last recorded transactions on the node for each account. It might be the case that the node has
        // the last transaction for an account but is missing one inbetween. In this case, a reconciliation of
        // transaction IDs must be performed.
        getLastRecordedNostroTransactions()
        // 4. Start the polling if auto-mode is enabled.
        if (cmdLineOptions.autoMode) {
            start()
        }
    }

    abstract fun start()

    abstract fun stop()

    companion object {
        val logger = loggerFor<Daemon>()
    }

    abstract fun scanForOpenBankingApiClients(): List<OpenBankingApi>

    private fun addAllBankAccounts() {
        val allAccounts = openBankingApiClients.flatMap { client -> client.accounts }
        println("\nAttempting to add bank account data to the issuer node...\n")
        allAccounts.forEach {
            val accountNumber = it.accountNumber as UKAccountNumber
            try {
                services.startFlowDynamic(AddBankAccount::class.java, it).returnValue.getOrThrow()
                println("\t* Added bank account with $accountNumber.")
            } catch (e: CordaRuntimeException) {
                println("\t* Bank account with $accountNumber has already been added.")
            }
        }
    }

    private fun getAllBalances(): List<Balance> {
        val nodeNostroBalances = services.startFlowDynamic(GetNostroAccountBalances::class.java).returnValue.getOrThrow()
        val bankNostroBalances = openBankingApiClients.flatMap { client ->
            client.accounts.map { account ->
                Pair(account.accountId, client.balance(account.accountId))
            }
        }.toMap()
        return bankNostroBalances.keys.map {
            val nodeBalance = nodeNostroBalances.getOrDefault(it, 0L)
            val bankBalance = bankNostroBalances[it]!!
            Balance(it, Amount(nodeBalance, bankBalance.token), bankBalance)
        }
    }

    private fun printBalances(balances: List<Balance>) {
        println("\nChecking bank balances for differences...\n")
        println("\tAccount ID\t\t\t\tNode Balance\t\tBank Balance\t\tDifference")
        println("\t----------\t\t\t\t------------\t\t------------\t\t----------")
        var totalDifference = 0L
        balances.forEach { (accountId, node, bank) ->
            val id = accountId.truncate()
            val difference = node.quantity - bank.quantity
            totalDifference += difference
            println("\t$id\t\t$node\t\t\t$bank\t\t\t$difference")
        }
        println()
        println("\t\t\t\t\t\t\t\t\t\t\tTotal difference: \t\t$totalDifference")
    }

    private fun getLastRecordedNostroTransactions() {
        // The daemon doesn't persist any data across restarts, so it must query the node to ascertain
        // the timestamps of the last recorded nostro transaction information. This is so the daemon
        // doesn't miss any transactions when it starts up after some downtime.
        if (cmdLineOptions.startFrom != null) {
            println("\nWill use \"start-from\" time if it is greater than the node's last timestamps...\n")
        } else {
            println("\nQuerying Issuer node for last recorded transactions per nostro account...\n")
        }
        val lastUpdatesByAccountId = services.startFlowDynamic(GetLastUpdatesByAccountId::class.java).returnValue.getOrThrow()
        if (lastUpdatesByAccountId.isNotEmpty()) {
            println("\tAccount ID\t\t\t\tTimestamp")
            println("\t----------\t\t\t\t---------")
            lastUpdatesByAccountId.forEach { (accountId, timestamp) ->
                // Use the start from timestamp if it was specified in the options and greater than the timestamps
                // of the last stored transactions in the node.
                val lastUpdate = cmdLineOptions.startFrom?.let {
                    if (it.toEpochMilli() > timestamp) it.toEpochMilli() else timestamp
                } ?: timestamp
                println("\t${accountId.truncate()}\t\t$lastUpdate")
                accountsToBank[accountId]?.updateLastTransactionTimestamps(accountId, lastUpdate)
                        ?: throw IllegalStateException("Issuer node has a last recorded transaction for $accountId. " +
                                "However, there is no corresponding bank API client!")
            }
        } else {
            println("\t* The node has no nostro transactions stored.")
            if (cmdLineOptions.startFrom != null) {
                println("\t* ... but we will use the \"start-from\" timestamp.")
                accountsToBank.forEach { t, _ ->
                    accountsToBank[t]!!.updateLastTransactionTimestamps(t, cmdLineOptions.startFrom.toEpochMilli())
                }
            }
        }
    }
}