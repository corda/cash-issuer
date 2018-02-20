package com.r3.cash.issuer.services

import com.r3.cash.issuer.base.types.RecordData
import com.r3.cash.issuer.base.types.RecordStatus
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class InMemoryMembershipService(val services: AppServiceHub) : MembershipService, SingletonSerializeAsToken() {

    private val membershipMap = hashMapOf<CordaX500Name, RecordData>()

    override fun hasRecord(recordKey: CordaX500Name): Boolean = membershipMap.containsKey(recordKey)

    override fun addNewRecord(recordKey: CordaX500Name): RecordData? {
        return membershipMap.putIfAbsent(recordKey, RecordData())
    }

    override fun updateRecord(recordKey: CordaX500Name, newStatus: RecordStatus): RecordData {
        val memberData = membershipMap[recordKey] ?: throw IllegalArgumentException("$recordKey is not in the membership list.")

        when (memberData.status) {
            RecordStatus.REQUESTED -> {
                if (newStatus in listOf(RecordStatus.SUSPENDED)) {
                    throw IllegalArgumentException("Cannot updated REQUESTED to SUSPENDED.")
                }
            }
            RecordStatus.APPROVED, RecordStatus.REJECTED -> {
                val invalidStatuses = setOf(RecordStatus.APPROVED, RecordStatus.REJECTED, RecordStatus.REQUESTED)
                if (newStatus in invalidStatuses) {
                    throw IllegalArgumentException("Cannot updated APPROVED or REJECTED to APPROVED, REJECTED OR REQUESTED.")
                }
            }
            RecordStatus.SUSPENDED -> throw IllegalArgumentException("Cannot updated SUSPENDED members.")
        }

        val updatedMembershipData = RecordData(newStatus)
        // Can never be null as record existence is checked above.
        return membershipMap.put(recordKey, updatedMembershipData)!!
    }

    override fun getRecordsByStatus(status: RecordStatus): Map<CordaX500Name, RecordData> {
        return membershipMap.filterValues { it.status == status }
    }

    override fun getRecord(recordKey: CordaX500Name): RecordData? {
        return membershipMap[recordKey]
    }

    override fun getRecords(): Map<CordaX500Name, RecordData> = membershipMap.toMap()

}