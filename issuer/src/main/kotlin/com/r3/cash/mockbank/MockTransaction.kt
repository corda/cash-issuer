package com.r3.cash.mockbank

import java.time.Instant
import java.util.*

enum class Direction { IN, OUT }

data class Counterparty(
        val name: String,
        val accountNumber: String,
        val sortCode: String
)

data class MockTransaction(
        val id: String,
        val amount: Long,
        val created: Instant,
        val direction: Direction,
        val counterparty: Counterparty,
        val description: String,
        val currency: Currency,
        val accountNumber: String
)