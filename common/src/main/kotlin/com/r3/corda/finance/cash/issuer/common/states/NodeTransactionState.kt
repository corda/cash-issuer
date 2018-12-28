package com.r3.corda.finance.cash.issuer.common.states

import com.r3.corda.finance.cash.issuer.common.contracts.NodeTransactionContract
import com.r3.corda.finance.cash.issuer.common.schemas.NodeTransactionStateSchemaV1
import com.r3.corda.finance.cash.issuer.common.types.NodeTransactionStatus
import com.r3.corda.finance.cash.issuer.common.types.NodeTransactionType
import net.corda.core.contracts.AmountTransfer
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant
import java.util.*

@BelongsToContract(NodeTransactionContract::class)
data class NodeTransactionState(
        val amountTransfer: AmountTransfer<Currency, Party>,
        val createdAt: Instant,
        override val participants: List<AbstractParty>,
        val notes: String,
        val type: NodeTransactionType,
        val status: NodeTransactionStatus = NodeTransactionStatus.PENDING,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is NodeTransactionStateSchemaV1 -> {
                NodeTransactionStateSchemaV1.PersistentNodeTransactionState(
                        issuer = amountTransfer.source.name.toString(),
                        counterparty = amountTransfer.destination.name.toString(),
                        notes = notes,
                        amount = amountTransfer.quantityDelta,
                        currency = amountTransfer.token.currencyCode,
                        type = type.name,
                        createdAt = createdAt.toEpochMilli(),
                        status = status.name,
                        linearId = linearId.id.toString()
                )
            }
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(NodeTransactionStateSchemaV1)
}