package com.r3.cash.monzo

import com.r3.cash.monzo.models.Accounts
import com.r3.cash.monzo.models.Balance
import com.r3.cash.monzo.models.Transaction
import com.r3.cash.monzo.models.Transactions

interface MonzoApi {

    /**
     * Returns a list of all accounts.
     */
    fun accounts(): Accounts

    /**
     * Returns the balance for a specific account ID.
     */
    fun balance(accountId: String): Balance

    /**
     * Returns a specific transaction by ID.
     */
    fun transaction(transactionId: String): Transaction

    /**
     * Returns a list of transactions. Multiple types of transactions are returned, for example: faster payments and
     * debit card payments. If [since] is specified then this method returns all transactions since the specified
     * transaction ID. If [limit] is specified then the number of transactions returned is limited.
     *
     * @accountId the account ID to list transactions for. With MonzoApi, users can have more than one account as those
     * who were BETA testers originally had pre-paid accounts which were then upgraded to bank accounts. Both accounts
     * are visible on the accounts list.
     * @limit the amount of transactions to show. The default is ZERO which returns up to 100 transactions. Transactions
     * are returned in chronological order.
     */
    fun transactions(accountId: String, since: String = "", amount: Int = 0): Transactions

}