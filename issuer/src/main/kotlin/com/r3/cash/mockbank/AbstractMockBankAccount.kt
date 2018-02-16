package com.r3.cash.mockbank

import com.r3.cash.MockTransactionGenerator
import net.corda.finance.GBP
import java.time.Instant
import java.util.*

/**
 * When a new transaction is added either manually or from the generator it must update the list of transactions
 * as well as the internal reconciliation.
 *
 * The internal reconciliation is used to keep track of who has deposited what in the account.
 */
abstract class AbstractMockBankAccount(
        override val id: String,
        final override val accountNumber: String,
        final override val sortCode: String,
        override val name: String,
        override val currency: Currency = GBP,
        override val created: Instant = Instant.now()
) : MockBankAccount {

    abstract val mockTransactionGenerator: MockTransactionGenerator
    private val _transactions: MutableList<MockTransaction> = Collections.synchronizedList<MockTransaction>(mutableListOf())

    init {
        require(sortCode.length == 6) { "Account numbers must be six digits long." }
        require(accountNumber.length == 8) { "Account numbers must be eight digits long." }
    }

    abstract fun recordTransaction(amount: Long, counterparty: Counterparty, direction: Direction, description: String)
    abstract fun generateIncomingTransaction(): () -> MockTransaction
    abstract fun startGeneratingTransactions(amount: Int = 0)

    fun recordTransaction(transaction: MockTransaction) = _transactions.add(transaction)

    override val transactions: List<MockTransaction> get() = _transactions
    override val balance: Long get() = transactions.map(MockTransaction::amount).sum()
    override fun toString() = "MonzoAccount(id=$id,name=$name,accountNumber=$accountNumber,sortCode=$sortCode,created=$created)"
}