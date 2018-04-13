package com.r3.corda.finance.cash.issuer.service.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object NodeTransactionStateSchema

object NodeTransactionStateSchemaV1 : MappedSchema(
        schemaFamily = NodeTransactionStateSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentNodeTransactionState::class.java)
) {

    @Entity
    @Table(name = "bank_accounts")
    class PersistentNodeTransactionState(
            @Column(name = "issuer")
            var issuer: String,
            @Column(name = "counterparty")
            var counterparty: String,
            @Column(name = "amount")
            var amount: Long,
            @Column(name = "currency")
            var currency: String,
            @Column(name = "created_at")
            var createdAt: Long,
            @Column(name = "type")
            var type: String,
            @Column(name = "linear_id")
            var linearId: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(
                issuer = "",
                counterparty = "",
                amount = 0L,
                currency = "",
                createdAt = 0L,
                type = "",
                linearId = ""
        )
    }

}