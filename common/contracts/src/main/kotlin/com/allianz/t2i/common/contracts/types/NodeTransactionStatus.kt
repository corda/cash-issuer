package com.allianz.t2i.common.contracts.types

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class NodeTransactionStatus {
    PENDING,
    COMPLETE
}