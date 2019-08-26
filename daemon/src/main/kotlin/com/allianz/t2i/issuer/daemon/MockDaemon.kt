package com.allianz.t2i.issuer.daemon

import com.allianz.t2i.issuer.daemon.mock.MockClient
import io.github.classgraph.ClassGraph
import net.corda.core.messaging.CordaRPCOps

private const val clientsPackage = "com.allianz.t2i.issuer.daemon.mock"

class MockDaemon(services: CordaRPCOps, options: CommandLineOptions) : AbstractDaemon(services, options) {
    override fun scanForOpenBankingApiClients(): List<OpenBankingApi> {
        println("Scanning the 'clients' package for Open Banking API clients...\n")

        val list = mutableListOf<OpenBankingApi>()
        ClassGraph().enableAllInfo().whitelistPackages(clientsPackage).scan().use { scanResult ->
            val sub = scanResult.getSubclasses(OpenBankingApi::class.java.name)
            sub.map {
                val apiName = it.simpleName
                val apiClient = it.loadClass().getDeclaredConstructor().newInstance()
                println("\t* Loaded $apiName API interface and client.")
                list.add(apiClient as OpenBankingApi)
            }
        }

        return list.toList()
    }

    override fun start() {
        openBankingApiClients.forEach {
            it as MockClient
            it.startGeneratingTransactions(0)
        }
        super.start()
    }

    override fun stop() {
        openBankingApiClients.forEach {
            it as MockClient
            it.stopGeneratingTransactions()
        }
        super.stop()
    }
}