package com.r3.cash.issuer.old

import com.r3.cash.issuer.daemon.monzo.MockMonzoApi
import com.r3.cash.issuer.daemon.monzo.MockMonzoBankAccount
import com.r3.cash.issuer.daemon.monzo.ProductionMonzoApi
import com.typesafe.config.ConfigFactory
import net.corda.node.internal.StartedNode
import net.corda.testing.node.MockNetwork
import org.junit.Before
import org.junit.Test

class MockBankTest : MockNetworkTestOld(numberOfNodes = 2) {

    lateinit var A: StartedNode<MockNetwork.MockNode>
    lateinit var B: StartedNode<MockNetwork.MockNode>
    lateinit var ISSUER: StartedNode<MockNetwork.MockNode>

    @Before
    override fun initialiseNodes() {
        A = nodes[0]
        B = nodes[1]
        ISSUER = nodes[2]
    }

    @Test
    fun `create a mock Monzo API, add an account and generate some transactions for 60 seconds then call the API`() {
        val mockMonzoApi = MockMonzoApi()
        mockMonzoApi.bank.openAccount("Roger", "12345678")
        val account = mockMonzoApi.bank.accounts.single() as MockMonzoBankAccount
        account.startGeneratingTransactions()
        Thread.sleep(20000)
        println(mockMonzoApi.transactions(account.id))
        println(mockMonzoApi.balance(account.id))
    }

    @Test
    fun `get transactions from the production monzo API`() {
        val config = ConfigFactory.parseResources("issuer.conf")
        val monzoAccessToken = config.getString("monzoAccessToken")
        val monzoApi = ProductionMonzoApi(monzoAccessToken, "https://api.monzo.com")
        println(monzoApi.accounts())
        println(monzoApi.balance(accountId = "acc_00009RE1DzwEupfetgm84f"))
        println(monzoApi.transactions(accountId = "acc_00009RE1DzwEupfetgm84f"))
        println(monzoApi.transactions(accountId = "acc_00009RE1DzwEupfetgm84f", since = "tx_00009RJHGnGd3LgsDVCmmn"))
    }

//    @Test
//    fun test() {
//        // Set up mock bank and api.
//        val bank = banks.single()
//        val mockMonzoApi = MockMonzoApi(bank)
//
//        // Inject the api.
//        val issuerService = ISSUER.services.cordaService(IssuerService::class.java)
//        issuerService.injectApi(mockMonzoApi)
//        issuerService.startPolling()
//        ISSUER.smm.changes.subscribe {
//            println(it)
//        }
//
//        network.waitQuiescent()
//
//        println(A.services.getCashBalance(GBP))
//        A.services.vaultService.queryBy<Cash.State>().states.forEach { cashState ->
//            println(cashState.state.data)
//        }
//    }

//    @Test
//    fun realTest() {
//        // Set up mock bank and api.
//        val config = ConfigFactory.parseResources("issuer.conf")
//        val monzoAccessToken = config.getString("monzoAccessToken")
//        val monzoApi = ProductionMonzoApi(monzoAccessToken, "https://api.monzo.com")
//
//        // Inject the api.
//        val issuerService = ISSUER.services.cordaService(IssuerService::class.java)
//        issuerService.injectApi(monzoApi)
//        issuerService.startPolling()
//        ISSUER.smm.changes.subscribe {
//            println(it)
//        }
//
//        network.waitQuiescent()
//
//        println(A.services.getCashBalance(GBP))
//        A.services.vaultService.queryBy<Cash.State>().states.forEach { cashState ->
//            println(cashState.state.data)
//        }
//    }

}