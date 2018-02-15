package com.r3.cash.monzo.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Counterparty(
        val sort_code: String = "null",
        val account_number: String = "null",
        val name: String = "null"
)