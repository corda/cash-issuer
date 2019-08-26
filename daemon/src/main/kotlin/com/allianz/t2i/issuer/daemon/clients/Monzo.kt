package com.allianz.t2i.issuer.daemon.clients

import com.allianz.t2i.common.contracts.types.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.allianz.t2i.issuer.daemon.*
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount
import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable
import rx.schedulers.Schedulers
import java.time.Instant

interface Monzo {
    @GET("/accounts")
    fun accounts(): Observable<MonzoAccounts>

    @GET("/balance")
    fun balance(@Query("account_id") accountId: String): Observable<MonzoBalance>

    @GET("/transactions")
    fun transactions(
            @Query("account_id") accountId: String,
            @Query("limit") limit: Int?,
            @Query("since") since: String?,
            @Query("before") before: String?
    ): Observable<MonzoTransactions>

    @GET("/transactions")
    fun transaction(@Query("transaction_id") transactionId: String): Observable<MonzoTransaction>
}

@Suppress("UNUSED")
class MonzoClient(configName: String) : OpenBankingApiClient(configName) {
    override val api: Monzo = OpenBankingApiFactory(Monzo::class.java, apiConfig, logger).build()

    private val _accounts: Map<BankAccountId, BankAccount> by lazy {
        accounts().map { it.accountId to it }.toMap()
    }

    override val accounts: List<BankAccount> = _accounts.values.toList()

    private fun accounts(): List<BankAccount> {
        // TODO: Filter accounts based upon those whitelisted in the config file.
        val accounts = wrapWithTry { api.accounts().getOrThrow().accounts.filter { !it.closed } }
        // Monzo doesn't provide the currency of its accounts.
        // For now they are all EUR but that might change...
        val currencies = wrapWithTry { accounts.map { balance(it.id).token } }
        require(currencies.size == accounts.size) { "Couldn't obtain currency information for all accounts." }
        // Join the currencies to the bank account data.
        val accountsWithCurrencies = accounts.zip(currencies)
        return accountsWithCurrencies.map { (account, currency) ->
            // TODO: Add the bank account type from the config file.
            account.toBankAccount(currency)
        }
    }

    override fun balance(accountId: BankAccountId?): Amount<TokenType> {
        if (accountId == null) throw IllegalArgumentException("AccountId is required for Monzo::balance.")
        val balance = wrapWithTry { api.balance(accountId).getOrThrow() }
        return Amount(balance.balance, balance.currency)
    }

    override fun transactionsFeed(): Observable<List<NostroTransaction>> {
        val transactions = accounts.map { account ->
            val accountId = account.accountId
            // For monzo, if we provide the last timestamp the API always returns the last transaction. So
            // here the timestamp in incremented by 1 millisecond.
            // TODO: Remove this hack and use the transaction ID instead.
            val lastTransactionTimestamp = lastTransactions[accountId]?.plusMillis(1L)
            wrapWithTry {
                api.transactions(account.accountId, null, lastTransactionTimestamp?.toString(), null).observeOn(Schedulers.io()).map {
                    it.transactions.map { transaction -> transaction.toNostroTransaction(ourAccount = account.accountNumber) }
                }
            }
        }

        // Merge and then flatten the feeds for all accounts.
        return Observable.merge(transactions)
    }
}

/** DATA TYPES */

data class MonzoAccounts(val accounts: List<MonzoAccount>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MonzoAccount(
        val id: String,
        val closed: Boolean,
        val description: String,
        val account_number: String?,
        val created: Instant,
        val sort_code: String?
)

fun MonzoAccount.toBankAccount(currency: TokenType): BankAccount {
    val accountNumber = if (account_number == null || sort_code == null) NoAccountNumber() else UKAccountNumber(sort_code, account_number)
    return BankAccount(id, description, accountNumber, currency)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MonzoBalance(val balance: Long, val currency: TokenType)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MonzoCounterparty(
        val sort_code: String?,
        val account_number: String?,
        val name: String?,
        val user_id: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MonzoTransaction(
        val account_id: String,
        val amount: Long,
        val created: Instant,
        val currency: TokenType,
        val description: String,
        val id: String,
        val notes: String,
        val scheme: String,
        val settled: Instant,
        val updated: Instant,
        val counterparty: MonzoCounterparty?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MonzoTransactions(val transactions: List<MonzoTransaction>)

// TODO: Refactor this.
fun MonzoTransaction.toNostroTransaction(ourAccount: AccountNumber): NostroTransaction {
    // Either we have an account number, or we don't.
    val theirAccount = counterparty?.let {
        if (it.sort_code == null || it.account_number == null) NoAccountNumber()
        else UKAccountNumber(it.sort_code, it.account_number)
    } ?: NoAccountNumber()

    // Monzo uses positive amounts for deposits and negative amounts for
    // withdrawals. Source and destination accounts are set accordingly.
    return if (amount > 0L) {
        NostroTransaction(id, account_id, amount, currency, scheme, description, created, theirAccount, ourAccount)
    } else {
        NostroTransaction(id, account_id, amount, currency, scheme, description, created, ourAccount, theirAccount)
    }
}

/**
override var lastUpdates = mapOf<String, Long>()
override val accounts: List<BankAccount> get() = _accounts.values.toList()



override fun transactions(accountId: String?, limit: Int?, since: String?, before: String?): List<NostroTransaction> {
if (accountId == null) throw IllegalArgumentException("AccountId is required for Monzo::transactions.")
return monzo.transactions(accountId, limit, since, before).getOrThrow().transactions.map {
require(accountId == it.account_id) { throw IllegalStateException("Account IDs should match.") }
// If HTTP request was successful, then 'accountId' should be a valid key in the _accounts map.
it.toNostroTransaction(_accounts[accountId]!!.accountNumber)
}
}

override fun transactionsFeed(): Observable<List<NostroTransaction>> {
// TODO: Filter out any closed accounts from 'since'.
val transactions = accounts.map { (accountId) ->
// There may or may not be a last update. If there is not, the
// lookup returns null and all transactions are polled for.
val lastUpdate = lastUpdates[accountId]
monzo.transactions(accountId, null, lastUpdate.toString(), null).observeOn(Schedulers.io()).map {
// _accounts will always be populated before this line executes.
it.transactions.map { it.toNostroTransaction(ourAccount = _accounts[accountId]!!.accountNumber) }
}
}

// Merge and then flatten the feeds for all accounts.
return Observable.merge(transactions)
}
 */