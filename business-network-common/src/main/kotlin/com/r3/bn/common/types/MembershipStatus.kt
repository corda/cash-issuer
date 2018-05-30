package com.r3.bn.common.types

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class MembershipStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    SUSPENDED
}