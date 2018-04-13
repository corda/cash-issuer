package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.r3.corda.finance.cash.issuer.common.types.NostroTransaction
import net.corda.core.contracts.Amount
import rx.Observable
import java.util.*

interface OpenBankingApiClient {

    val accounts: List<BankAccount>

    fun balance(accountId: String?): Amount<Currency>

    fun transactionsFeed(): Observable<List<NostroTransaction>>

    fun transactions(accountId: String?, limit: Int?, since: String?, before: String?): List<NostroTransaction>

}