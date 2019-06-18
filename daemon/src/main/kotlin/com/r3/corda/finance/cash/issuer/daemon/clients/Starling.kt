//package com.r3.corda.finance.cash.issuer.daemon.clients
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties
//import com.r3.corda.finance.cash.issuer.daemon.BankAccountId
//import com.r3.corda.finance.cash.issuer.daemon.OpenBankingApiClient
//import com.r3.corda.finance.cash.issuer.daemon.OpenBankingApiFactory
//import com.r3.corda.finance.cash.issuer.daemon.getOrThrow
//import com.r3.corda.lib.tokens.money.FiatCurrency
//import com.r3.corda.sdk.issuer.common.contracts.types.*
//import net.corda.core.contracts.Amount
//import retrofit2.http.GET
//import retrofit2.http.Path
//import retrofit2.http.Query
//import rx.Observable
//import rx.schedulers.Schedulers
//import java.math.BigDecimal
//import java.time.Instant
//import java.time.LocalDateTime
//import java.time.ZoneOffset
//import java.time.format.DateTimeFormatter
//
//interface Starling {
//    @GET("accounts")
//    fun accounts(): Observable<StarlingAccount>
//
//    @GET("accounts/balance")
//    fun balance(): Observable<StarlingBalance>
//
//    @GET("transactions")
//    fun transactions(@Query("from") from: String?, @Query("to") to: String?): Observable<StarlingTransactionBase>
//
//    @GET("transactions/{transactionId}")
//    fun transaction(@Path("id") transactionId: String): Observable<StarlingTransaction>
//
//    @GET("contacts/{contactId}/accounts/{accountId}")
//    fun contactAccount(@Path("contactId") contactId: String, @Path("accountId") accountId: String): Observable<StarlingContactAccount>
//
//    @GET("transactions/fps/out/{transactionId}")
//    fun fpsOut(@Path("transactionId") transactionId: String): Observable<StarlingFpsTransaction>
//
//    @GET("transactions/fps/in/{transactionId}")
//    fun fpsIn(@Path("transactionId") contactId: String): Observable<StarlingFpsTransaction>
//}
//
//@Suppress("UNUSED")
//class StarlingClient(configName: String) : OpenBankingApiClient(configName) {
//    override val api = OpenBankingApiFactory(Starling::class.java, apiConfig, logger)
//            .withAdditionalHeaders(mapOf("User-Agent" to "R3 Issuer Ltd"))
//            .build()
//
//    private val _accounts: Map<BankAccountId, BankAccount> by lazy {
//        accounts().map { it.accountId to it }.toMap()
//    }
//
//    override val accounts: List<BankAccount> = _accounts.values.toList()
//
//    fun accounts(): List<BankAccount> {
//        val account = api.accounts().getOrThrow()
//        return listOf(account.toBankAccount())
//    }
//
//    override fun balance(accountId: BankAccountId?): Amount<FiatCurrency> {
//        val balance = api.balance().getOrThrow()
//        // Amounts require cent/pence values.
//        return Amount((balance.amount * 100.toBigDecimal()).longValueExact(), balance.currency)
//    }
//
//    private fun getDateStringFromInstant(instant: Instant): String {
//        val date = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
//        val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//        return formattedDate.format(date)
//    }
//
//    override fun transactionsFeed(): Observable<List<NostroTransaction>> {
//        // There's only one account with Starling for the time being.
//        val lastTransaction = lastTransactions.values.singleOrNull()
//        val from = if (lastTransaction != null) getDateStringFromInstant(lastTransaction) else null
//        return api.transactions(from, null).observeOn(Schedulers.io()).map {
//            it._embedded.transactions.map {
//                toNostroTransaction(it)
//            }.filter {
//                // Starling only allows us to specify from which DAY we wish
//                // to query for transactions. As we have the last transaction
//                // timestamp we can filter out all the transactions we've
//                // already seen.
//                it.createdAt > (lastTransaction ?: Instant.EPOCH)
//            }
//        }
//    }
//
//    private fun toNostroTransaction(tx: StarlingTransaction): NostroTransaction {
//        // We must multiply the amount by 100 as Starling uses decimals.
//        val amount = tx.amount.toLong() * 100
//        val account = accounts.single()
//
//        // Function to get the account sort code and number as Starling doesn't provide it in the transaction data.
//        // It might be the case that no account number is available for some transactions.
//        fun getContactAccount(block: () -> StarlingFpsTransaction): AccountNumber {
//            val details = block()
//            return if (details.sendingContactId == null || details.sendingContactAccountId == null) {
//                NoAccountNumber()
//            } else {
//                val contactAccount = api.contactAccount(details.sendingContactId, details.sendingContactAccountId).getOrThrow()
//                UKAccountNumber(contactAccount.sortCode, contactAccount.accountNumber)
//            }
//        }
//
//        val (source, destination) = when (tx.direction) {
//            "INBOUND" -> {
//                // Get the account info.
//                val contactAccount = getContactAccount { api.fpsIn(tx.id).getOrThrow() }
//                Pair(contactAccount, account.accountNumber)
//            }
//            "OUTBOUND" -> {
//                val contactAccount = getContactAccount { api.fpsOut(tx.id).getOrThrow() }
//                Pair(account.accountNumber, contactAccount)
//            }
//            else -> throw IllegalStateException("This shouldn't happen.")
//        }
//
//        return NostroTransaction(tx.id, account.accountId, amount, tx.currency, tx.source, tx.narrative, tx.created, source, destination)
//    }
//}
//
///**
// * --------------------------------
// *** STARLING DATA MODELS BELOW ***
// * --------------------------------
// *
// * WARNING! THE DATA MODELS ARE NOT DOCUMENTED AND ARE NOT BACKWARDS COMPATIBLE SO THEY ARE EXPECTED TO BREAK SOMETIMES.
// * IF THEY DO THEN IT's LIKELY THAT SOMETHING UNDER HERE WILL NEED CHANGING. MOAN AT THEM ON SLACK IF YOU
// * NEED HELP: https://starlingdevs.slack.com/.
// */
//
///**
// * The model Starling returns for an account. Lots of information but most other APIs are not as generous, so we
// * discard most of this in order to achieve a common set of data for all APIs.
// */
//@JsonIgnoreProperties(ignoreUnknown = true)
//data class StarlingAccount(
//        val bic: String, // Not yet used.
//        val createdAt: Instant,
//        val currency: FiatCurrency,
//        val iban: String, // Not yet used.
//        val id: String,
//        val name: String,
//        val number: String, // Not yet used.
//        val accountNumber: String,
//        val sortCode: String
//)
//
//fun StarlingAccount.toBankAccount(): BankAccount {
//    return BankAccount(id, name, UKAccountNumber(sortCode, accountNumber), currency)
//}
//
///**
// * Again, lots of information but we discard most of it apart from amount and currency.
// */
//@JsonIgnoreProperties(ignoreUnknown = true)
//data class StarlingBalance(
//        val amount: BigDecimal,
//        val clearedBalance: BigDecimal,
//        val currency: FiatCurrency,
//        val effectiveBalance: BigDecimal,
//        val pendingTransactions: BigDecimal
//)
//
///**
// * Starling don't include contact information in their transaction models. You have to look it up separately. This is
// * the model representing a counterparty/contact account.
// */
//@JsonIgnoreProperties(ignoreUnknown = true)
//data class StarlingContactAccount(
//        val id: String,
//        val name: String,
//        val accountNumber: String,
//        val sortCode: String
//)
//
///** Starling embed their transaction data inside a nested mess of Json. */
//@JsonIgnoreProperties(ignoreUnknown = true)
//data class StarlingTransactionBase(val _links: Any, val _embedded: StarlingTransactions)
//
//data class StarlingTransactions(val transactions: List<StarlingTransaction>)
//
///**
// * The actual transaction model. Not useful as it doesn't tell us where the bloody money came from. To get that we need
// * to query the FPS endpoints.
// */
//@JsonIgnoreProperties(ignoreUnknown = true)
//data class StarlingTransaction(
//        val id: String,
//        val currency: FiatCurrency,
//        val amount: BigDecimal,
//        val direction: String,
//        val created: Instant,
//        val narrative: String,
//        val source: String
//)
//
///**
// * For getting the contact ID for counterparties/contacts.
// */
//@JsonIgnoreProperties(ignoreUnknown = true)
//data class StarlingFpsTransaction(val sendingContactId: String?, val sendingContactAccountId: String?)