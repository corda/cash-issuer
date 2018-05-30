package com.r3.cash.issuer.daemon.monzo

import com.r3.cash.issuer.daemon.MockTransactionGenerator
import com.r3.cash.issuer.daemon.counterparties
import com.r3.cash.issuer.daemon.generateRandomString
import com.r3.cash.issuer.daemon.mockbank.AbstractMockBankAccount
import com.r3.cash.issuer.daemon.mockbank.Counterparty
import com.r3.cash.issuer.daemon.mockbank.Direction
import com.r3.cash.issuer.daemon.mockbank.MockTransaction
import com.r3.cash.issuer.daemon.randomAmountGenerator
import net.corda.core.internal.randomOrNull
import java.time.Instant
import java.util.*

class MockMonzoBankAccount(
        id: String,
        accountNumber: String,
        sortCode: String,
        name: String,
        currency: Currency
) : AbstractMockBankAccount(id, accountNumber, sortCode, name, currency) {

    override val mockTransactionGenerator: MockTransactionGenerator = MockTransactionGenerator(generateIncomingTransaction())

    override fun recordTransaction(amount: Long, counterparty: Counterparty, direction: Direction, description: String) {
        val newTransaction = MockTransaction(
                id = "tx_00009${generateRandomString(16)}",
                amount = amount,
                direction = direction,
                counterparty = counterparty,
                created = Instant.now(),
                description = description,
                currency = currency,
                accountNumber = accountNumber
        )

        recordTransaction(newTransaction)
    }

    override fun generateIncomingTransaction(): () -> MockTransaction {
        return {
            MockTransaction(
                    id = "tx_00009${generateRandomString(16)}",
                    amount = randomAmountGenerator(),
                    direction = Direction.IN,
                    counterparty = counterparties.randomOrNull()!!,
                    created = Instant.now(),
                    description = "Funding for on-ledger cash.",
                    currency = currency,
                    accountNumber = accountNumber
            )
        }
    }

    override fun startGeneratingTransactions(amount: Int) {
        mockTransactionGenerator.start(amount).subscribe { recordTransaction(it) }
    }
}