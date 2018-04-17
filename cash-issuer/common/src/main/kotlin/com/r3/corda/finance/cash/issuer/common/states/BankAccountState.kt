package com.r3.corda.finance.cash.issuer.common.states

import com.r3.corda.finance.cash.issuer.common.schemas.BankAccountStateSchemaV1
import com.r3.corda.finance.cash.issuer.common.types.AccountNumber
import com.r3.corda.finance.cash.issuer.common.types.BankAccountType
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant
import java.util.*

// TODO: Maybe change verified property to whitelisted.
// The same principle applies to all accounts, customers and issuers.
// Only match transactions once the accounts have been verified/whitelisted.
data class BankAccountState(
        val owner: Party,
        val accountName: String,
        val accountNumber: AccountNumber,
        val currency: Currency,
        val type: BankAccountType,
        val verified: Boolean,
        override val participants: List<AbstractParty>,
        override val linearId: UniqueIdentifier,
        val lastUpdated: Instant = Instant.now()
) : LinearState, QueryableState {

    constructor(
            owner: Party,
            accountId: String,
            accountName: String,
            accountNumber: AccountNumber,
            currency: Currency,
            type: BankAccountType
    ) : this(owner, accountName, accountNumber, currency, type, false, listOf(owner), UniqueIdentifier(accountId))

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is BankAccountStateSchemaV1 -> BankAccountStateSchemaV1.PersistentBankAccountState(
                    owner = owner.name.toString(),
                    accountName = accountName,
                    accountNumber = accountNumber.digits,
                    currency = currency.currencyCode,
                    type = type.name,
                    verified = verified,
                    lastUpdated = lastUpdated.toEpochMilli(),
                    linearId = linearId.id.toString(),
                    externalId = linearId.externalId.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BankAccountStateSchemaV1)
}