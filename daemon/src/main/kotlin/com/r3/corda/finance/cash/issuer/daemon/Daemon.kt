package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.finance.cash.issuer.service.flows.AddNostroTransactions
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import net.corda.core.contracts.Amount
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.getOrThrow
import java.util.*

private val clientsPackage = "com.r3.corda.finance.cash.issuer.daemon.clients"

data class Balance(val accountId: BankAccountId, val nodeBalance: Amount<Currency>, val bankBalance: Amount<Currency>)

class Daemon(services: CordaRPCOps, options: CommandLineOptions) : AbstractDaemon(services, options) {
    override fun scanForOpenBankingApiClients(): List<OpenBankingApiClient> {
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

    override fun start() {
        subscriber = transactionsFeed.subscribe {
            if (it.isNotEmpty()) {
                println("Adding ${it.size} nostro transactions to the issuer node.")
                val addedTransactions = services.startFlowDynamic(AddNostroTransactions::class.java, it).returnValue.getOrThrow()
                addedTransactions.forEach { accountId, timestamp ->
                    // Update the last stored transaction timestamp.
                    accountsToBank[accountId]?.updateLastTransactionTimestamps(accountId, timestamp.toEpochMilli())
                    val bankApiName = accountsToBank[accountId]!!::class.java.simpleName
                    println("Updated $accountId for $bankApiName with the last seen timestamp $timestamp.")
                }
            } else {
                logger.info("Grabbed no transactions.")
            }
        }
    }

    override fun stop() {
        subscriber?.unsubscribe()
    }
}