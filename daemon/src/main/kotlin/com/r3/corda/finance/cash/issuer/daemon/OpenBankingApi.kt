package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.sdk.issuer.common.contracts.types.BankAccount
import com.r3.corda.sdk.issuer.common.contracts.types.NostroTransaction
import com.r3.corda.sdk.token.money.FiatCurrency
import net.corda.core.contracts.Amount
import rx.Observable
import java.time.Instant

abstract class OpenBankingApi {
    abstract val accounts: List<BankAccount>
    abstract fun balance(accountId: BankAccountId? = null): Amount<FiatCurrency>
    abstract fun transactionsFeed(): Observable<List<NostroTransaction>>
    val lastTransactions = mutableMapOf<BankAccountId, Instant>()
    fun updateLastTransactionTimestamps(accountId: BankAccountId, timestamp: Long) {
        lastTransactions[accountId] = Instant.ofEpochMilli(timestamp)
    }
}