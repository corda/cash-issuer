package com.allianz.t2i.common.contracts.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object NodeTransactionStateSchema

object NodeTransactionStateSchemaV1 : MappedSchema(
        schemaFamily = NodeTransactionStateSchema.javaClass,
        version = 1,
        mappedTypes = listOf(NodeTransactionStateSchemaV1.PersistentNodeTransactionState::class.java)
) {

    @Entity
    @Table(name = "node_transaction_states")
    class PersistentNodeTransactionState(
            @Column(name = "issuer")
            var issuer: String,
            @Column(name = "counterparty")
            var counterparty: String,
            @Column(name = "notes")
            var notes: String,
            @Column(name = "amount")
            var amount: Long,
            @Column(name = "currency")
            var currency: String,
            @Column(name = "created_at")
            var createdAt: Long,
            @Column(name = "type")
            var type: String,
            @Column(name = "status")
            var status: String,
            @Column(name = "linear_id")
            var linearId: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(
                issuer = "",
                counterparty = "",
                notes = "",
                amount = 0L,
                currency = "",
                createdAt = 0L,
                type = "",
                status = "",
                linearId = ""
        )
    }

}