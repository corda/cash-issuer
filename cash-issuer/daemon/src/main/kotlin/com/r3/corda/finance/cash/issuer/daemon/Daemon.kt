package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.finance.cash.issuer.common.flows.AddBankAccount
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import net.corda.core.messaging.CordaRPCOps

private val clientsPackage = "com.r3.corda.finance.cash.issuer.daemon.clients"

class Daemon(services: CordaRPCOps, options: CommandLineOptions) : AbstractDaemon(services, options) {

    // Search for all open banking api clients on the classpath.
    // To add additional clients, the daemon needs to be restarted.
    override val openBankingApiClients: List<OpenBankingApiClient> get() = scanForOpenBankingApiClients()

    init {
        // 1. Add all bank accounts for all API clients.
        addAllBankAccounts()
    }

    private fun scanForOpenBankingApiClients(): List<OpenBankingApiClient> {
        println("Scanning the 'clients' package for Open Banking API clients...")
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
                println("Loaded $apiName API interface and client.")
                add(apiClient)
            }.scan()
        }.toList()
    }

    private fun addAllBankAccounts() {
        openBankingApiClients.flatMap { client -> client.accounts }.forEach {
            services.startFlowDynamic(AddBankAccount::class.java, it)
        }
    }
}


/**
/** 2. Get all nostro account balances from the bank and the node. */
val nodeNostroBalances = services.startFlowDynamic(GetNostroAccountBalances::class.java).returnValue
val bankNostroBalances = openBankingApiClients.flatMap { client ->
client.accounts.map { account ->
Pair(account.accountId, client.balance(account.accountId))
}
}.toMap()

println(nodeNostroBalances.getOrThrow())
println(bankNostroBalances)
// TODO: Compare the balances and print them to the console.

/** 3. Get timestamps of the last recorded transaction for each nostro account. */
// The daemon doesn't persist any data across restarts, so it must query the node to ascertain
// the timestamps of the last recorded nostro transaction information. This is so the daemon
// doesn't miss any transactions when it starts up after some downtime.
val lastUpdatesByAccountId = services.startFlowDynamic(GetLastUpdatesByAccountId::class.java).returnValue.getOrThrow()
if (lastUpdatesByAccountId.isNotEmpty()) {
openBankingApiClients.forEach { client ->
val accountIds = client.accounts.toSet().map { it.accountId }
val relevantUpdates = lastUpdatesByAccountId.filter { it.key in accountIds }
client.lastUpdates = relevantUpdates
}
}

/** 4. Start polling the bank APIs for new transactions from the last update time. */
// Now the last transaction timestamps are updated, the daemon can start polling for
// new transactions from the correct time.
if (autoStart) {
startPolling()
}
}

private fun addBankAccounts() {
openBankingApiClients.flatMap { client -> client.accounts }.forEach {
logger.info("Adding banking account $it.")
services.startFlowDynamic(AddBankAccount::class.java, it)
// Upxate accountsToBank.
}
}

fun startPolling() {

}
 */