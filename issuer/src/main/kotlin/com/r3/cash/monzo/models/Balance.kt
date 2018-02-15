package com.r3.cash.monzo.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class Balance(
        val balance: Long,
        val total_balance: Long,
        val currency: Currency,
        val local_currency: Currency
)