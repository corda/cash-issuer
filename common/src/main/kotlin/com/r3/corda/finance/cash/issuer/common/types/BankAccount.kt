package com.r3.corda.finance.cash.issuer.common.types

import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.util.*

@CordaSerializable
data class BankAccount(
        val accountId: String,
        val accountName: String,
        val accountNumber: AccountNumber,
        val currency: Currency,
        val type: BankAccountType = BankAccountType.COLLATERAL // Defaulted to collateral for now.
)

fun BankAccount.toState(owner: Party, verifier: Party): BankAccountState {
    return BankAccountState(owner, verifier, accountId, accountName, accountNumber, currency, type)
}