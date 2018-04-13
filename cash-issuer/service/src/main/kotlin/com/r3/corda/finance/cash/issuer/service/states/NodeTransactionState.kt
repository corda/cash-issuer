package com.r3.corda.finance.cash.issuer.service.states

import com.r3.corda.finance.cash.issuer.service.schemas.NodeTransactionStateSchemaV1
import com.r3.corda.finance.cash.issuer.service.types.NodeTransactionType
import net.corda.core.contracts.AmountTransfer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant
import java.util.*

data class NodeTransactionState(
        val amountTransfer: AmountTransfer<Currency, Party>,
        val createdAt: Instant,
        override val participants: List<AbstractParty>,
        val type: NodeTransactionType = if (amountTransfer.quantityDelta > 0) NodeTransactionType.ISSUANCE else NodeTransactionType.REDEMPTION,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is NodeTransactionStateSchemaV1 -> NodeTransactionStateSchemaV1.PersistentNodeTransactionState(
                    issuer = amountTransfer.source.name.toString(),
                    counterparty = amountTransfer.destination.name.toString(),
                    amount = amountTransfer.quantityDelta,
                    currency = amountTransfer.token.currencyCode,
                    type = type.name,
                    createdAt = createdAt.toEpochMilli(),
                    linearId = linearId.id.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(NodeTransactionStateSchemaV1)
}