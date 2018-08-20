package com.r3.corda.finance.cash.issuer.common.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object BankAccountStateSchema

object BankAccountStateSchemaV1 : MappedSchema(
        schemaFamily = BankAccountStateSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentBankAccountState::class.java)
) {

    @Entity
    @Table(name = "bank_account_states")
    class PersistentBankAccountState(
            @Column(name = "owner")
            var owner: String,
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