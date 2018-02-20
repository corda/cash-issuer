package com.r3.cash.issuer.daemon.monzo

import com.r3.cash.issuer.daemon.mockbank.MockBank
import com.r3.cash.issuer.daemon.mockbank.MockTransaction
import com.r3.cash.issuer.daemon.monzo.models.*

class MockMonzoApi(val bank: MockBank = MockMonzoBank()) : MonzoApi {

    override fun accounts(): Accounts {
        val accounts = bank.accounts.map { account ->
            Account(
                    account_number = account.accountNumber,
                    created = account.created,
                    sort_code = account.sortCode,
                    description = account.name,
                    id = account.id,
                    type = AccountType.uk_retail
            )
        }

        return Accounts(accounts = accounts)
    }

    override fun balance(accountId: String): Balance {
        val account = account(accountId)

        return Balance(
                balance = account.balance,
                currency = account.currency,
                local_currency = account.currency,
                total_balance = account.balance
        )
    }

    override fun transactions(accountId: String, since: String, amount: Int): Transactions {
        val account = account(accountId)
        val getAll = since == "" && amount == 0

        val results = when {
            getAll -> account.transactions
            amount > 0 -> account.transactions.takeLast(amount)
            since != "" -> account.transactions.takeLastWhile { it.id != since }
            since == "" && amount == 0 -> account.transactions.takeLastWhile { it.id != since }.takeLast(amount)
            else -> throw IllegalArgumentException("Something went horribly wrong!")
        }

        return Transactions(transactions = results.map { transformTransaction(it) })
    }

    override fun transaction(transactionId: String): Transaction {
        val transaction = bank.accounts.flatMap { it.transactions }.single { it.id == transactionId }
        return transformTransaction(transaction)
    }

    private fun account(accountId: String) = bank.account(accountId) as MockMonzoBankAccount

    private fun transformTransaction(tx: MockTransaction): Transaction {
        return Transaction(
                account_id = tx.accountNumber,
                amount = tx.amount,
                attachments = listOf(),
                counterparty = Counterparty(
                        name = tx.counterparty.name,
                        account_number = tx.counterparty.accountNumber,
                        sort_code = tx.counterparty.sortCode
                ),
                currency = tx.currency,
                created = tx.created,
                description = tx.description,
                id = tx.id,
                local_amount = tx.amount,
                local_currency = tx.currency,
                notes = "",
                scheme = "payport_faster_payments",
                settled = tx.created,
                updated = tx.created
        )
    }

}
