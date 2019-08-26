package com.allianz.t2i.common.contracts.types

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class NostroTransactionStatus {
    UNMATCHED,
    MATCHED_ISSUER,
    MATCHED_COUNTERPARTY,
    MATCHED
}