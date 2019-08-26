package com.allianz.t2i.issuer.daemon

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.allianz.t2i.common.contracts.types.BankAccount
import com.allianz.t2i.common.contracts.types.NostroTransaction
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