package com.r3.corda.finance.cash.issuer.service.types

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class NostroTransactionStatus {
    UNMATCHED,
    MATCHED_ISSUER,
    MATCHED_COUNTERPARTY,
    MATCHED
}