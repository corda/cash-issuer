package com.r3.corda.finance.cash.issuer.daemon.monzo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.r3.corda.finance.cash.issuer.common.types.*
import java.time.Instant
import java.util.*

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

fun MonzoAccount.toBankAccount(currency: Currency): BankAccount {
    val accountNumber = if (account_number == null || sort_code == null) NoAccountNumber() else UKAccountNumber(account_number, sort_code)
    return BankAccount(id, description, accountNumber, currency)
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MonzoBalance(val balance: Long, val currency: Currency)

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
        val currency: Currency,
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
        else UKAccountNumber(it.account_number, it.sort_code)
    } ?: NoAccountNumber()

    // Monzo uses positive amounts for deposits and negative amounts for
    // withdrawals. Source and destination accounts are set accordingly.
    return if (amount > 0L) {
        NostroTransaction(id, account_id, amount, currency, scheme, description, created, theirAccount, ourAccount)
    } else {
        NostroTransaction(id, account_id, amount, currency, scheme, description, created, ourAccount, theirAccount)
    }
}