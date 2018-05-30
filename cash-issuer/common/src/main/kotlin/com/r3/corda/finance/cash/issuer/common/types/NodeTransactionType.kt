package com.r3.corda.finance.cash.issuer.common.types

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class NodeTransactionType {
    ISSUANCE,
    REDEMPTION
}