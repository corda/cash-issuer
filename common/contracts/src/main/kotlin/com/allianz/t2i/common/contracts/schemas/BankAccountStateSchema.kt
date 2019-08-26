package com.allianz.t2i.common.contracts.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object BankAccountStateSchema

object BankAccountStateSchemaV1 : MappedSchema(
        schemaFamily = BankAccountStateSchema.javaClass,
        version = 1,
        mappedTypes = listOf(BankAccountStateSchemaV1.PersistentBankAccountState::class.java)
) {

    @Entity
    @Table(name = "bank_account_states")
    class PersistentBankAccountState(
            @Column(name = "owner")
            var owner: String,
            @Column(name = "verifier")
            var verifier: String,
            @Column(name = "accountName")
            var accountName: String,
            @Column(name = "accountNumber")
            var accountNumber: String,
            @Column(name = "currency")
            var currency: String,
            @Column(name = "type")
            var type: String,
            @Column(name = "verified")
            var verified: Boolean,
            @Column(name = "last_updated")
            var lastUpdated: Long,
            @Column(name = "linear_id")
            var linearId: String,
            @Column(name = "external_id")
            var externalId: String
    ) : PersistentState() {
        @Suppress("UNUSED")
        constructor() : this(
                owner = "",
                verifier = "",
                accountName = "",
                accountNumber = "",
                currency = "",
                type = "",
                verified = false,
                lastUpdated = 0L,
                linearId = "",
                externalId = ""
        )
    }

}