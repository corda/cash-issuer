package com.r3.cash.issuer.daemon.monzo.models

import java.time.Instant

data class Account(
        val account_number: String = "null", // Pre-paid accounts don't have account numbers.
        val created: Instant,
        val description: String,
        val id: String,
        val sort_code: String = "null", // Pre-paid accounts don't have account numbers.
        val type: AccountType
)