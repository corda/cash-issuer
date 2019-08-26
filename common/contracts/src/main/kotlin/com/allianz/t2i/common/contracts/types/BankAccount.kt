package com.allianz.t2i.common.contracts.types

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.allianz.t2i.common.contracts.states.BankAccountState
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class BankAccount(
        val accountId: String,
        val accountName: String,
        val accountNumber: AccountNumber,
        val currency: TokenType,
        val type: BankAccountType = BankAccountType.COLLATERAL // Defaulted to collateral for now.
)

fun BankAccount.toState(owner: Party, verifier: Party): BankAccountState {
    return BankAccountState(owner, verifier, accountId, accountName, accountNumber, currency, type)
}