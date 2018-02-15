package com.r3.cash.monzo.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class Transaction(
        val account_id: String,
        val amount: Long,
        val attachments: List<Any>,
        val counterparty: Counterparty,
        val created: Instant,
        val currency: Currency,
        val description: String,
        val id: String,
        val local_amount: Long,
        val local_currency: Currency,
        val notes: String,
        val scheme: String,
        val settled: Instant,
        val updated: Instant
)