package com.r3.cash.monzo

import com.r3.cash.generateRandomString
import com.r3.cash.mockbank.MockBank
import com.r3.cash.mockbank.MockBankAccount
import net.corda.core.internal.randomOrNull
import java.util.*

class MockMonzoBank : MockBank {

    override val accounts: Collection<MockBankAccount> get() = _accounts
    override val sortCodes: List<String> get() = listOf("040040")

    private val _accounts: MutableSet<MockBankAccount> = mutableSetOf()

    override fun addAccount(id: String, accountNumber: String, sortCode: String, name: String, currency: Currency): MockBank {
        val newAccount = MockMonzoBankAccount(id, accountNumber, sortCode, name, currency)
        _accounts.add(newAccount)
        return this
    }

    override fun addAccount(name: String, accountNumber: String): MockBank {
        return addAccount("acc_00009${generateRandomString(16)}", accountNumber, sortCodes.randomOrNull()!!, name)
    }

    override fun account(id: String): MockBankAccount? {
        return accounts.singleOrNull { bankAccount -> bankAccount.id == id }
    }

}