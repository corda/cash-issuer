package com.r3.corda.sdk.issuer.common.contracts.states

import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.sdk.issuer.common.contracts.NodeTransactionContract
import com.r3.corda.sdk.issuer.common.contracts.schemas.NodeTransactionStateSchemaV1
import com.r3.corda.sdk.issuer.common.contracts.types.NodeTransactionStatus
import com.r3.corda.sdk.issuer.common.contracts.types.NodeTransactionType
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

@BelongsToContract(NodeTransactionContract::class)
data class NodeTransactionState(
        val amountTransfer: AmountTransfer<FiatCurrency, Party>,
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
                        currency = amountTransfer.token.tokenIdentifier,
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