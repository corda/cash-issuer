package com.r3.corda.finance.cash.issuer.daemon.mock

import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.r3.corda.finance.cash.issuer.common.types.NostroTransaction
import com.r3.corda.finance.cash.issuer.daemon.BankAccountId
import com.r3.corda.finance.cash.issuer.daemon.OpenBankingApiClient
import net.corda.core.contracts.Amount
import rx.Observable
import java.util.*

class Mock {

}

class MockClient(configName: String) : OpenBankingApiClient(configName) {
    override val api: Any get() = Mock()
    override val accounts: List<BankAccount> get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun balance(accountId: BankAccountId?): Amount<Currency> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun transactionsFeed(): Observable<List<NostroTransaction>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

//val mockMonzoAccount = BankAccount(
//        accountId = "acc_00009RE1DzwEupfetgm84f",
//        accountName = "R3 Ltd",
//        accountNumber = UKAccountNumber("97784499", "040004"),
//        currency = Currency.getInstance("GBP")
//)
//
//val mockMonzoCounterparties = listOf(
//        MockContact("anonuser_e4d0fc5b4693fc16219ef7", "Roger Willis", UKAccountNumber("12538727", "442200")),
//        MockContact("anonuser_qb9hcjpaem61mocujv3zh4", "David Nicol", UKAccountNumber("41558501", "873456")),
//        MockContact("anonuser_x636uuqj1b913bd1mflm61", "Joel Dudley", UKAccountNumber("73510753", "059015")),
//        MockContact("anonuser_keu8gr5fs4qw6kj4nufy91", "Richard Geeen", UKAccountNumber("34782115", "022346")),
//        MockContact("anonuser_z1oxucxi9ooep90oteb4qw", "Cais Manai", UKAccountNumber("90143578", "040040"))
//)

///**
// * A mock version of the Monzo API that allows us to specify a mock bank account, mock transactions to emit and mock
// * counterparties up front, so we can perform assertions over results for testing.
// *
// * If the list of transactions is empty, then we'll generate transactions. Otherwise, they will be taken from the
// * list provided.
// */
//class MockMonzo(
//        override val accountDetails: BankAccount,
//        override val contacts: List<MockContact> = emptyList(),
//        override val fastForward: Boolean = false
//) : OpenBankingApi, MockApi() {
//
//    init {
//        require(contacts.isNotEmpty()) { "You must supply some mock contacts." }
//    }
//
//    /** A list of mock transactions that have been generated or pre-supplied. */
//    override val transactions: MutableList<NostroTransaction> = Collections.synchronizedList<NostroTransaction>(mutableListOf())
//
//    override val transactionGenerator: MockTransactionGenerator = MockTransactionGenerator(mockTransactionGenerator(this, true), fastForward)
//
//    /**
//     * We need to keep a record of which contacts have depositied what balances, so that we don't withdraw more than
//     * they have deposited. If they could, then the mock transactions wouldn't really make sense for testing purposes.
//     */
//    override val contactBalances: MutableMap<MockContact, Long> = Collections.synchronizedMap<MockContact, Long>(hashMapOf())
//
//    /** Only return the one account. */
//    override fun account(accountId: String): BankAccount? {
//        return if (accountId == accountDetails.accountId) accountDetails else null
//    }
//
//    /** Only return our specified mock account */
//    override fun accounts(): List<BankAccount> {
//        return listOf(accountDetails)
//    }
//
//    override fun balance(accountId: String?): Amount<Currency> {
//        if (accountId == null) throw IllegalArgumentException("You must specify an accountId.")
//        val balance = transactions.map(NostroTransaction::amount).sum()
//        return Amount(balance, accountDetails.currency)
//    }
//
//    override fun transactions(accountId: String?, since: Instant?, before: Instant?, limit: Int?): List<NostroTransaction> {
//        if (accountId == null) throw IllegalArgumentException("You must specify an accountId.")
//        val getOneHundred = since == null && before == null && limit == null
//        val window = since != null && before != null
//        return when {
//            getOneHundred -> transactions.take(100)
//            limit != null -> transactions.take(limit)
//            since != null -> transactions.takeLastWhile { it.createdAt > since }
//            before != null -> transactions.takeWhile { it.createdAt < before }
//            window -> transactions.takeLastWhile { it.createdAt > since }.takeWhile { it.createdAt < before }
//            else -> throw IllegalArgumentException("Something went horribly wrong!")
//        }
//    }
//
//    override fun addManuallyEnteredTransaction(nostroTransaction: NostroTransaction) {
//        transactions.add(nostroTransaction)
//    }
//
//    override fun startGeneratingTransactions(numberToGenerate: Int) {
//        transactionGenerator.start(numberToGenerate) { transactions.add(it) }
//    }
//
//    override fun stopGeneratingTransactions() {
//        transactionGenerator.stop()
//    }
//
//    override fun transaction(transactionId: String): NostroTransaction? {
//        return transactions.single { it.transactionId == transactionId }
//    }
//
//}

//class MockStarling(
//        override val accountDetails: BankAccount,
//        override val contacts: List<MockContact> = emptyList(),
//        override val fastForward: Boolean = false
//) : OpenBankingApi, MockApi() {
//
//    init {
//        require(contacts.isNotEmpty()) { "You must supply some mock contacts." }
//    }
//
//    /** A list of mock transactions that have been generated or pre-supplied. */
//    override val transactions: MutableList<NostroTransaction> = Collections.synchronizedList<NostroTransaction>(mutableListOf())
//
//    override val transactionGenerator: MockTransactionGenerator = MockTransactionGenerator(mockTransactionGenerator(this, true), fastForward)
//
//    /**
//     * We need to keep a record of which contacts have depositied what balances, so that we don't withdraw more than
//     * they have deposited. If they could, then the mock transactions wouldn't really make sense for testing purposes.
//     */
//    override val contactBalances: MutableMap<MockContact, Long> = Collections.synchronizedMap<MockContact, Long>(hashMapOf())
//
//    /** Only return the one account. */
//    override fun account(accountId: String): BankAccount? {
//        return if (accountId == accountDetails.accountId) accountDetails else null
//    }
//
//    /** Only return our specified mock account */
//    override fun accounts(): List<BankAccount> {
//        return listOf(accountDetails)
//    }
//
//    override fun balance(accountId: String?): Amount<Currency> {
//        val balance = transactions.map(NostroTransaction::amount).sum()
//        return Amount(balance, accountDetails.currency)
//    }
//
//    override fun transactions(accountId: String?, since: Instant?, before: Instant?, limit: Int?): List<NostroTransaction> {
//        val getOneHundred = since == null && before == null && limit == null
//
//        fun date(instant: Instant): LocalDate {
//            val dateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)
//            return LocalDate.of(dateTime.year, dateTime.monthValue, dateTime.dayOfMonth)
//        }
//
//        // If since and before are set then return a window. If nothing is set, return 100. Else if since OR before
//        // are set then handle those. Lastly, handle limit.
//        return when {
//            since != null && before != null -> {
//                transactions.takeLastWhile { date(it.createdAt) > date(since) }.takeWhile { date(it.createdAt) < date(before) }
//            }
//            getOneHundred -> transactions.take(100)
//            since != null -> transactions.takeLastWhile { date(it.createdAt) >= date(since) }
//            before != null -> transactions.takeWhile { date(it.createdAt) <= date(before) }
//            limit != null -> transactions.take(limit)
//            else -> throw IllegalArgumentException("Something went horribly wrong!")
//        }
//    }
//
//    override fun transaction(transactionId: String): NostroTransaction? {
//        return transactions.single { it.transactionId == transactionId }
//    }
//
//    override fun addManuallyEnteredTransaction(nostroTransaction: NostroTransaction) {
//        transactions.add(nostroTransaction)
//    }
//
//    override fun startGeneratingTransactions(numberToGenerate: Int) {
//        transactionGenerator.start(numberToGenerate) { transactions.add(it) }
//    }
//
//    override fun stopGeneratingTransactions() {
//        transactionGenerator.stop()
//    }
//
//}
