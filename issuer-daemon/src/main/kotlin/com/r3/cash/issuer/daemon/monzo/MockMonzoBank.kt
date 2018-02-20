package com.r3.cash.issuer.daemon.monzo

import com.r3.cash.issuer.daemon.generateRandomString
import com.r3.cash.issuer.daemon.mockbank.MockBank
import com.r3.cash.issuer.daemon.mockbank.MockBankAccount
import net.corda.core.internal.randomOrNull
import java.util.*

class MockMonzoBank : MockBank {

    override val accounts: Collection<MockBankAccount> get() = _accounts
    override val sortCodes: List<String> get() = listOf("040040")

    private val _accounts: MutableSet<MockBankAccount> = mutableSetOf()

    override fun openAccount(id: String, accountNumber: String, sortCode: String, name: String, currency: Currency): MockBankAccount {
        val newAccount = MockMonzoBankAccount(id, accountNumber, sortCode, name, currency)
        _accounts.add(newAccount)
        return newAccount
    }

    override fun openAccount(name: String, accountNumber: String): MockBankAccount {
        return openAccount("acc_00009${generateRandomString(16)}", accountNumber, sortCodes.randomOrNull()!!, name)
    }

    override fun account(id: String): MockBankAccount? {
        return accounts.singleOrNull { bankAccount -> bankAccount.id == id }
    }

}