package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.r3.corda.finance.cash.issuer.common.types.NostroTransaction
import net.corda.core.contracts.Amount
import rx.Observable
import java.time.Instant
import java.util.*

abstract class OpenBankingApi {
    abstract val accounts: List<BankAccount>
    abstract fun balance(accountId: BankAccountId? = null): Amount<Currency>
    abstract fun transactionsFeed(): Observable<List<NostroTransaction>>
    val lastTransactions = mutableMapOf<BankAccountId, Instant>()
    fun updateLastTransactionTimestamps(accountId: BankAccountId, timestamp: Long) {
        lastTransactions[accountId] = Instant.ofEpochMilli(timestamp)
    }
}