package com.r3.corda.finance.cash.issuer.service.types

import com.r3.corda.finance.cash.issuer.common.types.AccountNumber
import com.r3.corda.finance.cash.issuer.service.states.NostroTransactionState
import net.corda.core.contracts.AmountTransfer
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.util.*

@CordaSerializable
data class NostroTransaction(
        val transactionId: String,
        val accountId: String,
        val amount: Long,
        val currency: Currency,
        val type: String,
        val description: String,
        val bank: String,
        val createdAt: Instant,
        val source: AccountNumber,
        val destination: AccountNumber
)

fun NostroTransaction.toState(issuer: Party): NostroTransactionState {
    return NostroTransactionState(
            issuer = issuer,
            transactionId = transactionId,
            amountTransfer = AmountTransfer(
                    quantityDelta = amount,
                    token = currency,
                    source = source,
                    destination = destination
            ),
            description = description,
            createdAt = createdAt
    )
}