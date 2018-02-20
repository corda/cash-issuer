package com.r3.cash.issuer.daemon.mockbank

import net.corda.finance.GBP
import java.util.*

interface MockBank {
    val sortCodes: Collection<String>
    val accounts: Collection<MockBankAccount>

    fun openAccount(name: String, accountNumber: String): MockBankAccount
    fun openAccount(id: String, accountNumber: String, sortCode: String, name: String, currency: Currency = GBP): MockBankAccount
    fun account(id: String): MockBankAccount?
}