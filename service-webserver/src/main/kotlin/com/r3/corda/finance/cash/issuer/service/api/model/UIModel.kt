package com.r3.corda.finance.cash.issuer.service.api.model

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.states.NodeTransactionState
import com.r3.corda.finance.cash.issuer.common.states.NostroTransactionState
import com.r3.corda.finance.cash.issuer.common.types.*
import com.r3.corda.finance.cash.issuer.service.helpers.getBigDecimalFromLong
import javafx.collections.ObservableList
import net.corda.core.contracts.ContractState
import net.corda.core.serialization.CordaSerializable
import net.corda.finance.contracts.asset.Cash.State
import java.math.BigDecimal
import java.time.Instant
import java.util.*


@CordaSerializable
@Suspendable
data class CashUIModel(
        val amount: BigDecimal,
        val amountlong: Long,
        val currency: String,
        val owner: String,
        val issuer: String
)
fun State.toUiModel(): CashUIModel {
    return CashUIModel(
            amount = getBigDecimalFromLong(amount.quantity),
            amountlong = amount.quantity,
            currency = amount.token.product.currencyCode,
            owner = owner.toString(),
            issuer = amount.token.issuer.party.toString()
    )
}

@CordaSerializable
@Suspendable
data class BankAccountUiModel(
        val owner: String,
        val internalAccountId: UUID,
        val externalAccountId: String,
        val accountName: String,
        val accountNumber: AccountNumber,
        val currency: Currency,
        val type: BankAccountType,
        val verified: Boolean,
        val lastUpdated: Instant
)
fun BankAccountState.toUiModel(): BankAccountUiModel {
    return BankAccountUiModel(
            owner.toString(),
            linearId.id,
            linearId.externalId!!,
            accountName,
            accountNumber,
            currency,
            type,
            verified,
            lastUpdated
    )
}

@CordaSerializable
@Suspendable
data class NostroTransactionUiModel(
        val internalTransactionId: UUID,
        val accountId: String,
        val amount: Long,
        val currency: Currency,
        val source: AccountNumber,
        val destination: AccountNumber,
        val createdAt: Instant,
        val status: NostroTransactionStatus,
        val type: NostroTransactionType,
        val lastUpdated: Instant
)

fun NostroTransactionState.toUiModel(): NostroTransactionUiModel {
    return NostroTransactionUiModel(
            linearId.id,
            accountId,
            amountTransfer.quantityDelta,
            amountTransfer.token,
            amountTransfer.source,
            amountTransfer.destination,
            createdAt,
            status,
            type,
            lastUpdated
    )
}

@CordaSerializable
@Suspendable
data class NodeTransactionUiModel(
        val internalTransactionId: UUID,
        val amount: Long,
        val currency: Currency,
        val source: String,
        val destination: String,
        val notes: String,
        val createdAt: Instant,
        val status: NodeTransactionStatus,
        val type: NodeTransactionType
)

fun NodeTransactionState.toUiModel(): NodeTransactionUiModel {
    return NodeTransactionUiModel(
            linearId.id,
            amountTransfer.quantityDelta,
            amountTransfer.token,
            amountTransfer.source.toString(),
            amountTransfer.destination.toString(),
            notes,
            createdAt,
            status,
            type
    )
}


fun <T : ContractState, U : Any> ObservableList<T>.transform(block: (T) -> U) = map { block(it) }