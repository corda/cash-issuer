package com.r3.cash.mockbank

import net.corda.finance.GBP
import java.util.*

interface MockBank {
    val sortCodes: Collection<String>
    val accounts: Collection<MockBankAccount>

    fun addAccount(name: String, accountNumber: String): MockBank
    fun addAccount(id: String, accountNumber: String, sortCode: String, name: String, currency: Currency = GBP): MockBank
    fun account(id: String): MockBankAccount?
}