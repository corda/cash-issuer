package com.r3.corda.finance.cash.issuer.common.states

import com.r3.corda.finance.cash.issuer.common.schemas.NostroTransactionStateSchemaV1
import com.r3.corda.finance.cash.issuer.common.types.AccountNumber
import com.r3.corda.finance.cash.issuer.common.types.NostroTransactionStatus
import com.r3.corda.finance.cash.issuer.common.types.NostroTransactionType
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

data class NostroTransactionState(
        val accountId: String,
        val amountTransfer: AmountTransfer<Currency, AccountNumber>,
        val description: String,
        val createdAt: Instant,
        override val participants: List<AbstractParty>,
        override val linearId: UniqueIdentifier,
        val lastUpdated: Instant = createdAt,
        val status: NostroTransactionStatus = NostroTransactionStatus.UNMATCHED,
        val type: NostroTransactionType = NostroTransactionType.UNKNOWN
) : LinearState, QueryableState {

    constructor(
            accountId: String,
            issuer: Party,
            transactionId: String,
            amountTransfer: AmountTransfer<Currency, AccountNumber>,
            description: String,
            createdAt: Instant
    ) : this(accountId, amountTransfer, description, createdAt, listOf(issuer), UniqueIdentifier(transactionId))

    fun updateStatus(newStatus: NostroTransactionStatus): NostroTransactionState = copy(status = newStatus)
    fun updateType(newType: NostroTransactionType): NostroTransactionState = copy(type = newType)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is NostroTransactionStateSchemaV1 -> NostroTransactionStateSchemaV1.PersistentNostroTransactionState(
                    accountId = accountId,
                    transactionId = linearId.externalId ?: throw IllegalStateException("This should never be null."),
                    amount = amountTransfer.quantityDelta,
                    currency = amountTransfer.token.currencyCode,
                    sourceAccountNumber = amountTransfer.source.digits,
                    destinationAccountNumber = amountTransfer.destination.digits,
                    lastUpdated = lastUpdated.toEpochMilli(),
                    status = status.name,
                    type = type.name,
                    createdAt = createdAt.toEpochMilli(),
                    linearId = linearId.id.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(NostroTransactionStateSchemaV1)
}