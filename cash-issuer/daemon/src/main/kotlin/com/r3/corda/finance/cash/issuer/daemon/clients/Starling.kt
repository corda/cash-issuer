package com.r3.corda.finance.cash.issuer.daemon.clients

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.r3.corda.finance.cash.issuer.common.types.NoAccountNumber
import com.r3.corda.finance.cash.issuer.common.types.NostroTransaction
import com.r3.corda.finance.cash.issuer.common.types.UKAccountNumber
import com.r3.corda.finance.cash.issuer.daemon.BankAccountId
import com.r3.corda.finance.cash.issuer.daemon.OpenBankingApiClient
import com.r3.corda.finance.cash.issuer.daemon.OpenBankingApiFactory
import com.r3.corda.finance.cash.issuer.daemon.getOrThrow
import net.corda.core.contracts.Amount
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import rx.Observable
import rx.schedulers.Schedulers
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

interface Starling {
    @GET("accounts")
    fun accounts(): Observable<StarlingAccount>

    @GET("accounts/balance")
    fun balance(): Observable<StarlingBalance>

    @GET("transactions")
    fun transactions(@Query("from") from: String?, @Query("to") to: String?): Observable<StarlingTransactionBase>

    @GET("transactions/{transactionId}")
    fun transaction(@Path("id") transactionId: String): Observable<StarlingTransaction>

    @GET("contacts/{contactId}/accounts/{accountId}")
    fun contactAccount(@Path("contactId") contactId: String, @Path("accountId") accountId: String): Observable<StarlingContactAccount>

    @GET("transactions/fps/out/{transactionId}")
    fun fpsOut(@Path("transactionId") transactionId: String): Observable<StarlingFpsOutTransaction>

    @GET("transactions/fps/in/{transactionId}")
    fun fpsIn(@Path("transactionId") contactId: String): Observable<StarlingFpsInTransaction>
}

@Suppress("UNUSED")
class StarlingClient(configName: String) : OpenBankingApiClient(configName) {
    override val api = OpenBankingApiFactory(Starling::class.java, apiConfig)
            .withAdditionalHeaders(mapOf("User-Agent" to "R3 Issuer Ltd"))
            .build()

    private val _accounts: Map<BankAccountId, BankAccount> by lazy {
        accounts().map { it.accountId to it }.toMap()
    }

    override val accounts: List<BankAccount> = _accounts.values.toList()

    fun accounts(): List<BankAccount> {
        val account = api.accounts().getOrThrow()
        return listOf(account.toBankAccount())
    }

    override fun balance(accountId: BankAccountId?): Amount<Currency> {
        val balance = api.balance().getOrThrow()
        // Amounts require cent/pence values.
        return Amount(balance.amount.longValueExact() * 100, balance.currency)
    }

    private fun getDateStringFromInstant(instant: Instant): String {
        val date = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return formattedDate.format(date)
    }

    override fun transactionsFeed(): Observable<List<NostroTransaction>> {
        // There's only one account with Starling for the time being.
        val lastTransaction = lastTransactions.values.singleOrNull()
        val from = if (lastTransaction != null) {
            getDateStringFromInstant(lastTransaction)
        } else {
            null
        }
        return api.transactions(from, null).observeOn(Schedulers.io()).map {
            it._embedded.transactions.map {
                toNostroTransaction(it)
            }.filter {
                // Starling only allows us to specify from which DAY we wish
                // to query for transactions. As we have the last transaction
                // timestamp we can filter out all the transactions we've
                // already seen.
                // TODO: Fix this from throwing an NPE (see that it is null above if no previous transactions have been stored.
                // TODO: Also fix the monzo API from returning one transaction. Get the API to return the Nostro transaction state as opposed to one property of it.
                it.createdAt > lastTransaction
            }
        }
    }

    private fun toNostroTransaction(tx: StarlingTransaction): NostroTransaction {
        // We must multiply the amount by 100 as Starling uses decimals.
        val amount = tx.amount.toLong() * 100
        val account = accounts.single()
        return when (tx.source) {
            "FASTER_PAYMENTS_IN" -> {
                // Get the counterparty IDs.
                val txDetail = api.fpsIn(tx.id).getOrThrow()
                // Get the account info.
                val contactDetail = api.contactAccount(txDetail.sendingContactId, txDetail.sendingContactAccountId).getOrThrow()
                val contactAccount = UKAccountNumber(contactDetail.accountNumber, contactDetail.sortCode)
                NostroTransaction(tx.id, account.accountId, amount, tx.currency, tx.source, tx.narrative, tx.created, contactAccount, account.accountNumber)
            }
            "FASTER_PAYMENTS_OUT" -> {
                val txDetail = api.fpsOut(tx.id).getOrThrow()
                val contactDetail = api.contactAccount(txDetail.receivingContactId, txDetail.receivingContactAccountId).getOrThrow()
                val contactAccount = UKAccountNumber(contactDetail.accountNumber, contactDetail.sortCode)
                NostroTransaction(tx.id, account.accountId, amount, tx.currency, tx.source, tx.narrative, tx.created, account.accountNumber, contactAccount)
            }
            else -> {
                // If it's not a faster payment then we probably don't have the counterparty account details, so we
                // just add our account details and the rest stays null. Not much else we can do unless the party that
                // is sending cash to Starling account provides us their account number and sort code before they make
                // the transfer.
                if (tx.direction == "INBOUND") {
                    NostroTransaction(tx.id, account.accountId, amount, tx.currency, tx.source, tx.narrative, tx.created, NoAccountNumber(), account.accountNumber)
                } else {
                    NostroTransaction(tx.id, account.accountId, amount, tx.currency, tx.source, tx.narrative, tx.created, account.accountNumber, NoAccountNumber())
                }
            }
        }
    }
}

/**
 * --------------------------------
 *** STARLING DATA MODELS BELOW ***
 * --------------------------------
 *
 * WARNING! THE DATA MODELS ARE NOT DOCUMENTED AND ARE NOT BACKWARDS COMPATIBLE SO THEY ARE EXPECTED TO BREAK SOMETIMES.
 * IF THEY DO THEN IT's LIKELY THAT SOMETHING UNDER HERE WILL NEED CHANGING. MOAN AT THEM ON SLACK IF YOU
 * NEED HELP: https://starlingdevs.slack.com/.
 */

/**
 * The model Starling returns for an account. Lots of information but most other APIs are not as generous, so we
 * discard most of this in order to achieve a common set of data for all APIs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingAccount(
        val bic: String, // Not yet used.
        val createdAt: Instant,
        val currency: Currency,
        val iban: String, // Not yet used.
        val id: String,
        val name: String,
        val number: String, // Not yet used.
        val accountNumber: String,
        val sortCode: String
)

fun StarlingAccount.toBankAccount(): BankAccount {
    return BankAccount(id, name, UKAccountNumber(sortCode, accountNumber), currency)
}

/**
 * Again, lots of information but we discard most of it apart from amount and currency.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingBalance(
        val amount: BigDecimal,
        val clearedBalance: BigDecimal,
        val currency: Currency,
        val effectiveBalance: BigDecimal,
        val pendingTransactions: BigDecimal
)

/**
 * Starling don't include contact information in their transaction models. You have to look it up separately. This is
 * the model representing a counterparty/contact account.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingContactAccount(
        val id: String,
        val name: String,
        val accountNumber: String,
        val sortCode: String
)

/** Starling embed their transaction data inside a nested mess of Json. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingTransactionBase(val _links: Any, val _embedded: StarlingTransactions)

data class StarlingTransactions(val transactions: List<StarlingTransaction>)

/**
 * The actual transaction model. Not useful as it doesn't tell us where the bloody money came from. To get that we need
 * to query the FPS endpoints.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingTransaction(
        val id: String,
        val currency: Currency,
        val amount: BigDecimal,
        val direction: String,
        val created: Instant,
        val narrative: String,
        val source: String
)

/**
 * Two endpoints for getting the contact ID for counterparties/contacts.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingFpsInTransaction(val sendingContactId: String, val sendingContactAccountId: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StarlingFpsOutTransaction(val receivingContactId: String, val receivingContactAccountId: String)