package com.allianz.t2i.common.contracts.schemas

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object NostroTransactionStateSchema

object NostroTransactionStateSchemaV1 : MappedSchema(
        schemaFamily = NostroTransactionStateSchema.javaClass,
        version = 1,
        mappedTypes = listOf(NostroTransactionStateSchemaV1.PersistentNostroTransactionState::class.java)
) {

    @Entity
    @Table(name = "nostro_transaction_States")
    class PersistentNostroTransactionState(
            @Column(name = "id")
            var transactionId: String,
            @Column(name = "account_id")
            var accountId: String,
            @Column(name = "amount")
            var amount: Long,
            @Column(name = "currency")
            var currency: String,
            @Column(name = "source_account_number")
            var sourceAccountNumber: String,
            @Column(name = "destination_account_number")
            var destinationAccountNumber: String,
            @Column(name = "created_at")
            var createdAt: Long,
            @Column(name = "last_updated")
            var lastUpdated: Long,
            @Column(name = "type")
            var type: String,
            @Column(name = "status")
            var status: String,
            @Column(name = "linear_id")
            var linearId: String
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(
                accountId = "",
                transactionId = "",
                amount = 0L,
                currency = "",
                sourceAccountNumber = "",
                destinationAccountNumber = "",
                createdAt = 0L,
                lastUpdated = 0L,
                type = "",
                status = "",
                linearId = ""
        )
    }

}