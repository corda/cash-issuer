package com.r3.cash.issuer.services

import com.r3.cash.issuer.flows.IssueCash
import com.r3.cash.monzo.MonzoApi
import com.r3.cash.monzo.models.AccountType
import com.r3.cash.monzo.models.Transaction
import com.r3.cash.monzo.models.Transactions
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.getOrThrow
import net.corda.finance.contracts.asset.Cash
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

@CordaService
class IssuerService(private val services: AppServiceHub) : SingletonSerializeAsToken() {

    lateinit var mockMonzoApi: MonzoApi

    private val storedTransactions = Collections.synchronizedList<Transaction>(mutableListOf())

    fun injectApi(api: MonzoApi) {
        mockMonzoApi = api
    }

    private fun getIncrementalTxs(accountId: String): Transactions {
        return if (storedTransactions.isEmpty()) {
            mockMonzoApi.transactions(accountId)
        } else {
            val lastTx = storedTransactions.last().id
            mockMonzoApi.transactions(accountId, lastTx)
        }
    }

    fun startPolling(interval: Long = 5L, times: Int = 10) {
        // Transactions which the issuer has stored.

        val accountId = mockMonzoApi.accounts().accounts.single { it.type == AccountType.uk_retail }.id
        val consumer = Observable.interval(interval, TimeUnit.SECONDS).map { getIncrementalTxs(accountId) }

        // TODO: Put back .take()
        consumer.observeOn(Schedulers.newThread()).toBlocking().subscribe {
            it.transactions.forEach {
                println("${it.account_id} ${it.id} ${it.currency} ${it.amount} ")
                val stx = services.startFlow(IssueCash(it.amount)).returnValue.getOrThrow()
                val output = stx.tx.outputsOfType<Cash.State>().single()
                println(output)
            }

            storedTransactions.addAll(it.transactions)
        }

        println(storedTransactions)
    }

}