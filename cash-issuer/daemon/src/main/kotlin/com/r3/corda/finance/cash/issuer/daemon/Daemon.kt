package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.finance.cash.issuer.common.flows.AddBankAccount
import com.r3.corda.finance.cash.issuer.common.types.UKAccountNumber
import com.r3.corda.finance.cash.issuer.service.flows.AddNostroTransactions
import com.r3.corda.finance.cash.issuer.service.flows.GetLastUpdatesByAccountId
import com.r3.corda.finance.cash.issuer.service.flows.GetNostroAccountBalances
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import net.corda.core.contracts.Amount
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.getOrThrow
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

private val clientsPackage = "com.r3.corda.finance.cash.issuer.daemon.clients"

data class Balance(val accountId: BankAccountId, val nodeBalance: Amount<Currency>, val bankBalance: Amount<Currency>)

class Daemon(services: CordaRPCOps, options: CommandLineOptions) : AbstractDaemon(services, options) {

    // Search for all open banking api clients on the classpath.
    // To add additional clients, the daemon needs to be restarted.
    override val openBankingApiClients: List<OpenBankingApiClient> by lazy { scanForOpenBankingApiClients() }

    // Maps bank account IDs to the open banking api client which provides access.
    private val accountsToBank: Map<BankAccountId, OpenBankingApiClient> by lazy {
        mutableMapOf<BankAccountId, OpenBankingApiClient>().apply {
            openBankingApiClients.flatMap { client ->
                client.accounts.map { account ->
                    put(account.accountId, client)
                }
            }
        }
    }

    init {
        // 1. Add all bank accounts for all API clients.
        addAllBankAccounts()
        // 2. Query nostro balances on Corda and via the APIs.
        val balances = getAllBalances()
        printBalances(balances)
        getLastRecordedNostroTransactions()
    }

    private var subscriber: Subscription? = null
    private val transactionsFeed = Observable
            .interval(5, TimeUnit.SECONDS, Schedulers.io())
            .flatMap { Observable.merge(openBankingApiClients.map(OpenBankingApiClient::transactionsFeed)) }
            .doOnError { println(it.message) }

    fun start() {
        subscriber = transactionsFeed.subscribe {
            println(it)
            if (it.isNotEmpty()) {
                println("Adding ${it.size} nostro transactions to the issuer node.")
                val addedTransactions = services.startFlowDynamic(AddNostroTransactions::class.java, it).returnValue.getOrThrow()
                addedTransactions.forEach { accountId, timestamp ->
                    // Update the last stored transaction timestamp.
                    // For monzo, if we provide the last timestamp the API always returns the last transaction. So
                    // here the timestamp in incremented by 1 millisecond.
                    // TODO: Remove this hack and use the transaction ID instead.
                    accountsToBank[accountId]?.updateLastTransactionTimestamps(accountId, timestamp.plusMillis(1L).toEpochMilli())
                    val bankApiName = accountsToBank[accountId]!!::class.java.simpleName
                    println("Updated $accountId for $bankApiName with the last seen timestamp $timestamp.")
                }
            } else {
                logger.info("Grabbed no transactions.")
            }
        }
    }

    fun stop() = subscriber?.unsubscribe()

    private fun scanForOpenBankingApiClients(): List<OpenBankingApiClient> {
        println("Scanning the 'clients' package for Open Banking API clients...\n")
        val fastClasspathScanner = FastClasspathScanner(clientsPackage)
        return mutableListOf<OpenBankingApiClient>().apply {
            fastClasspathScanner.matchSubclassesOf(OpenBankingApiClient::class.java) {
                if (!it.simpleName.endsWith("Client")) {
                    throw IllegalStateException("Your bank API client's name must be suffixed with \"Client\".")
                }
                val apiName = it.simpleName.removeSuffix("Client")
                // Check to see that an API interface definition has been provided for Retrofit. It is expected that
                // the API interface has the same name as the client minus the "Client" suffix.
                try {
                    Class.forName("$clientsPackage.$apiName")
                } catch (e: ClassNotFoundException) {
                    throw IllegalStateException("For each bank API you must implement an API interface for Retrofit " +
                            "and an associated client. E.g. \"Monzo\" for the interface and \"MonzoClient\" for the " +
                            "client. In addition an associated config file is required for each bank API. E.g. For " +
                            "Monzo a config file called \"monzo.conf\" is required.")
                }
                val apiClient = it.getDeclaredConstructor(String::class.java).newInstance(apiName.toLowerCase())
                println("\t* Loaded $apiName API interface and client.")
                add(apiClient)
            }.scan()
        }.toList()
    }

    private fun addAllBankAccounts() {
        val allAccounts = openBankingApiClients.flatMap { client -> client.accounts }
        println("\nAttempting to add bank account data to the issuer node...\n")
        allAccounts.forEach {
            val accountNumber = it.accountNumber as UKAccountNumber
            try {
                services.startFlowDynamic(AddBankAccount::class.java, it).returnValue.getOrThrow()
                println("\t* Added bank account with $accountNumber.")
            } catch (e: IllegalArgumentException) {
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
        println("\t--------------\t\t\t------------\t\t------------\t\t----------")
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
        println("\nQuerying Issuer node for last recorded transactions per nostro account...\n")
        val lastUpdatesByAccountId = services.startFlowDynamic(GetLastUpdatesByAccountId::class.java).returnValue.getOrThrow()
        if (lastUpdatesByAccountId.isNotEmpty()) {
            println("\tAccount Number\t\t\tTimestamp")
            println("\t--------------\t\t\t---------")
            lastUpdatesByAccountId.forEach { (accountId, timestamp) ->
                println("\t${accountId.truncate()}\t\t$timestamp")
                accountsToBank[accountId]?.updateLastTransactionTimestamps(accountId, timestamp)
                        ?: throw IllegalStateException("Issuer node has a last recorded transaction for $accountId. " +
                                "However, there is no corresponding bank API client!")
            }
        } else {
            println("\t* The node has no nostro transactions stored.")
        }
    }
}