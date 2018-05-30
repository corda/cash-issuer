package com.r3.cash.issuer.common.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object BankAccountRecordSchema

object BankAccountRecordSchemaV1 : MappedSchema(
        schemaFamily = BankAccountRecordSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBankAccountRecord::class.java)
) {

    @Entity
    @Table(name = "bank_account_records")
    class PersistentBankAccountRecord(
            @Column(name = "account_owner")
            var accountOwner: String,
            @Column(name = "issuer")
            var issuer: String,
            @Column(name = "accountName")
            var accountName: String,
            @Column(name = "account_number")
            var accountNumber: String,
            @Column(name = "sort_code")
            var sortCode: String,
            @Column(name = "verified")
            var verified: Boolean,
            @Column(name = "last_updated")
            var lastUpdated: Instant,
            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(
                accountOwner = "",
                issuer = "",
                accountName = "",
                accountNumber = "",
                sortCode = "",
                verified = false,
                lastUpdated = Instant.now(),
                linearId = UUID.randomUUID()
        )
    }

}