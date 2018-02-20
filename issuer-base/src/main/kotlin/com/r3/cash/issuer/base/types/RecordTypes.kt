package com.r3.cash.issuer.base.types

import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
enum class RecordStatus {
    REQUESTED, APPROVED, REJECTED, SUSPENDED
}

@CordaSerializable
data class RecordData(
        var status: RecordStatus = RecordStatus.REQUESTED,
        var lastUpdated: Instant = Instant.now()
)

@CordaSerializable
data class RecordStatusUpdateNotification(
        val previousStatus: RecordStatus,
        val newStatus: RecordStatus
)

@CordaSerializable
sealed class RequestToJoinBusinessNetworkResponse {

    class Success : RequestToJoinBusinessNetworkResponse() {
        override fun toString() = "Request to join succeeded."
    }

    data class Failure(val existingRecord: RecordData) : RequestToJoinBusinessNetworkResponse()

}