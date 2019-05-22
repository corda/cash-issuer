package com.r3.corda.sdk.issuer.common.contracts.types

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class NodeTransactionStatus {
    PENDING,
    COMPLETE
}