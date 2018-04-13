package com.r3.corda.finance.cash.issuer.daemon.monzo

import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.r3.corda.finance.cash.issuer.common.types.NostroTransaction
import com.r3.corda.finance.cash.issuer.daemon.OpenBankingApiClient
import com.r3.corda.finance.cash.issuer.daemon.RestClientFactory
import com.r3.corda.finance.cash.issuer.daemon.getOrThrow
import net.corda.core.contracts.Amount
import rx.Observable
import rx.schedulers.Schedulers
import java.time.Instant
import java.util.*

// TODO: Add a flag for whether to auto poll upon starting up. Or not. Instead perhaps we have to manually start it, via start.

class MonzoApiClient : OpenBankingApiClient {

    private val monzo: Monzo = RestClientFactory(Monzo::class.java).build()
    private val _accounts = mutableMapOf<String, BankAccount>()

    private fun accounts(): List<BankAccount> {
        // Monzo returns closed accounts, which should be discarded.
        val accounts = monzo.accounts().getOrThrow().accounts.filter { !it.closed }
        val balances = accounts.map { balance(it.id).token }
        // Join the currencies to the bank account data
        // then return a list of bank account objects.
        val accountsWithBalances = accounts.zip(balances)
        return accountsWithBalances.map { (account, currency) ->
            val accountWithCurrency = account.toBankAccount(currency)
            _accounts.putIfAbsent(accountWithCurrency.accountId, accountWithCurrency)
            accountWithCurrency
        }
    }

    init {
        accounts()
    }

    var lastUpdates = mapOf<String, Long>()
    override val accounts: List<BankAccount> get() = _accounts.values.toList()

    override fun balance(accountId: String?): Amount<Currency> {
        if (accountId == null) throw IllegalArgumentException("AccountId is required for Monzo::balance.")
        val balance = monzo.balance(accountId).getOrThrow()
        return Amount(balance.balance, balance.currency)
    }

    override fun transactions(accountId: String?, limit: Int?, since: String?, before: String?): List<NostroTransaction> {
        if (accountId == null) throw IllegalArgumentException("AccountId is required for Monzo::transactions.")
        return monzo.transactions(accountId, limit, since, before).getOrThrow().transactions.map {
            require(accountId == it.account_id) { throw IllegalStateException("Account IDs should match.") }
            // If HTTP request was successful, then 'accountId' should be a valid key in the _accounts map.
            it.toNostroTransaction(_accounts[accountId]!!.accountNumber)
        }
    }

    // So we need some kind of default value.
    override fun transactionsFeed(): Observable<List<NostroTransaction>> {
        // TODO: Filter out any closed accounts from 'since'.
        val transactions = lastTransactions.map { (lastTransactionTimestamp, accountId) ->
            val timestamp = Instant.ofEpochSecond(lastTransactionTimestamp)
            monzo.transactions(accountId, null, timestamp.toString(), null).observeOn(Schedulers.io()).map {
                // _accounts will always be populated before this line executes.
                it.transactions.map { it.toNostroTransaction(ourAccount = _accounts[accountId]!!.accountNumber) }
            }
        }

        // Merge and then flatten the feeds for all accounts.
        return Observable.merge(transactions)
    }

}