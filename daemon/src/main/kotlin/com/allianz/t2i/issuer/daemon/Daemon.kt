package com.allianz.t2i.issuer.daemon

import com.r3.corda.lib.tokens.contracts.types.TokenType
import io.github.classgraph.ClassGraph
import net.corda.core.contracts.Amount
import net.corda.core.messaging.CordaRPCOps
import java.lang.reflect.InvocationTargetException


private val clientsPackage = "com.r3.corda.finance.cash.issuer.daemon.clients"

data class Balance(val accountId: BankAccountId, val nodeBalance: Amount<TokenType>, val bankBalance: Amount<TokenType>)

class Daemon(services: CordaRPCOps, options: CommandLineOptions) : AbstractDaemon(services, options) {
    override fun scanForOpenBankingApiClients(): List<OpenBankingApi> {
        println("Scanning the 'clients' package for Open Banking API clients...\n")

        val list = mutableListOf<OpenBankingApi>()
        ClassGraph().enableAllInfo().whitelistPackages(clientsPackage).scan().use { scanResult ->
            scanResult.getSubclasses(OpenBankingApiClient::class.java.name).map {
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

                println("\t* Loaded $apiName API interface and client.")
                val apiClient = try {
                    it.loadClass().getDeclaredConstructor(String::class.java).newInstance(apiName.toLowerCase())
                } catch (e: InvocationTargetException) {
                    throw RuntimeException("Creating open banking API client failed. The most likely reason is bad credentials. " +
                            "Check your API key. If you don't have a monzo or starling account, then run the daemon in --mock-mode")
                }
                list.add(apiClient as OpenBankingApi)
            }
        }
        return list.toList()
    }

}