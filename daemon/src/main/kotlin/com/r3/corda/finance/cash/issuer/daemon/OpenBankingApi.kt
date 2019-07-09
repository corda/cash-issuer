package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.sdk.issuer.common.contracts.types.BankAccount
import com.r3.corda.sdk.issuer.common.contracts.types.NostroTransaction
import net.corda.core.contracts.Amount
import rx.Observable
import java.time.Instant

abstract class OpenBankingApi {
    abstract val accounts: List<BankAccount>
    abstract fun balance(accountId: BankAccountId? = null): Amount<TokenType>
    abstract fun transactionsFeed(): Observable<List<NostroTransaction>>
    val lastTransactions = mutableMapOf<BankAccountId, Instant>()
    fun updateLastTransactionTimestamps(accountId: BankAccountId, timestamp: Long) {
        lastTransactions[accountId] = Instant.ofEpochMilli(timestamp)
    }
}