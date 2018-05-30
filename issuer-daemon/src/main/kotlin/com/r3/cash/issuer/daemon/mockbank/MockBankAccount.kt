package com.r3.cash.issuer.daemon.mockbank

import java.time.Instant
import java.util.*

interface MockBankAccount {
    val id: String
    val transactions: List<MockTransaction>
    val balance: Long
    val accountNumber: String
    val sortCode: String
    val name: String
    val created: Instant
    val currency: Currency
}