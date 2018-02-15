package com.r3.cash

import com.r3.cash.monzo.MockMonzoApi
import com.r3.cash.monzo.MockMonzoBankAccount
import com.r3.cash.monzo.ProductionMonzoApi
import com.typesafe.config.ConfigFactory
import org.junit.Test

class MockBankTest {

//    private val mockApi = MockMonzoApi()

//    @Before
//    fun setup() {
//        mockApi.simulate()
//    }

//    @Test
//    fun test() {
//        val accountNumber = mockApi.accounts().accounts.single().account_number
//        println("Generate transactions for ten seconds...")
//        Thread.sleep(10000)
//        val transactions = mockApi.transactions(accountNumber)
//        println("First: " + transactions.transactions.first().id)
//        println("Last: " + transactions.transactions.last().id)
//        println("Getting the transaction id of the last transaction generated.")
//        val lastTxId = mockApi.transactions(accountNumber).transactions.last().id
//        println("Last transaction id: $lastTxId")
//        println("Generate transactions for five more seconds...")
//        Thread.sleep(5000)
//        val transactionsSince = mockApi.transactions(accountNumber, lastTxId).transactions
//        println("Print the transaction since transaction with id: $lastTxId")
//        println(transactionsSince)
//        println(mockApi.balance(accountNumber))
//    }

    @Test
    fun `create a mock Monzo API, add an account and generate some transactions for 60 seconds then call the API`() {
        val mockMonzoApi = MockMonzoApi()
        mockMonzoApi.bank.addAccount("Roger", "12345678")
        val account = mockMonzoApi.bank.accounts.single() as MockMonzoBankAccount
        val subscriber = account.startGeneratingTransactions()
        Thread.sleep(20000)
        subscriber.unsubscribe()
        println(mockMonzoApi.transactions(account.id))
        println(mockMonzoApi.balance(account.id))
    }

    @Test
    fun `get transactions from the prduction monzo API`() {
        val config = ConfigFactory.parseResources("issuer.conf")
        val monzoAccessToken = config.getString("monzoAccessToken")
        val monzoApi = ProductionMonzoApi(monzoAccessToken, "https://api.monzo.com")
        println(monzoApi.accounts())
        println(monzoApi.balance(accountId = "acc_00009RE1DzwEupfetgm84f"))
        println(monzoApi.transactions(accountId = "acc_00009RE1DzwEupfetgm84f"))
        println(monzoApi.transactions(accountId = "acc_00009RE1DzwEupfetgm84f", since = "tx_00009RJHGnGd3LgsDVCmmn"))
    }

}