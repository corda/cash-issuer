package com.allianz.t2i.common.contracts.types

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.allianz.t2i.common.contracts.states.NostroTransactionState
import net.corda.core.contracts.AmountTransfer
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.time.Instant

@CordaSerializable
data class NostroTransaction(
        val transactionId: String,
        val accountId: String,
        val amount: Long,
        val currency: TokenType,
        val type: String,
        val description: String,
        val createdAt: Instant,
        val source: AccountNumber,
        val destination: AccountNumber
)

fun NostroTransaction.toState(issuer: Party): NostroTransactionState {
    return NostroTransactionState(
            accountId = accountId,
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