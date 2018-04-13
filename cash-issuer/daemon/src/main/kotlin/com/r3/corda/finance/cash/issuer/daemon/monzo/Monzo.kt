package com.r3.corda.finance.cash.issuer.daemon.monzo

import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

interface Monzo {
    @GET("/accounts")
    fun accounts(): Observable<MonzoAccounts>

    @GET("/balance")
    fun balance(@Query("account_id") accountId: String): Observable<MonzoBalance>

    @GET("/transactions")
    fun transactions(@Query("account_id") accountId: String, @Query("limit") limit: Int?, @Query("since") since: String?, @Query("before") before: String?): Observable<MonzoTransactions>

    @GET("/transactions")
    fun transaction(@Query("transaction_id") transactionId: String): Observable<MonzoTransaction>
}