package com.r3.cash.issuer.services

import com.r3.cash.issuer.base.types.RecordData
import com.r3.cash.issuer.base.types.RecordStatus
import net.corda.core.identity.CordaX500Name

interface MembershipService {

    /**
     * Checks to see if there is a record for a specific X500Name. The existence of a record does not imply membership
     * of the cash business network. Returns true if the record exists and false if it doesn't.
     */
    fun hasRecord(recordKey: CordaX500Name): Boolean

    /**
     * Adds a new record to the membership service. New records are always added with the status 'REQUESTED'. If a
     * record for the specified X500Name already exists, this function will return the existing record.
     */
    fun addNewRecord(recordKey: CordaX500Name): RecordData?

    /**
     * Allows an existing record in the membership service to be updated. Not all updates are valid. For example, an
     * 'APPROVED' status cannot be updated to 'REQUESTED'. If the update was successful, the function returns the
     * previous membership data.
     *
     * @throws IllegalArgumentException if the state transition is invalid.
     */
    @Throws(IllegalArgumentException::class)
    fun updateRecord(recordKey: CordaX500Name, newStatus: RecordStatus): RecordData

    /**
     * Returns all records with the specified status.
     */
    fun getRecordsByStatus(status: RecordStatus): Map<CordaX500Name, RecordData>

    /**
     * Gets the record for the specified X500Name.
     */
    fun getRecord(recordKey: CordaX500Name): RecordData?

    /**
     * Returns all records.
     */
    fun getRecords(): Map<CordaX500Name, RecordData>

}