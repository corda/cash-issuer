package com.allianz.t2i.common.contracts.states

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.allianz.t2i.common.contracts.BankAccountContract
import com.allianz.t2i.common.contracts.schemas.BankAccountStateSchemaV1
import com.allianz.t2i.common.contracts.serializers.TokenTypeSerializer
import com.allianz.t2i.common.contracts.types.AccountNumber
import com.allianz.t2i.common.contracts.types.BankAccountType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.time.Instant


// The same principle applies to all accounts, customers and issuers.
// Only match transactions once the accounts have been verified/whitelisted.
@BelongsToContract(BankAccountContract::class)
data class BankAccountState(
        val owner: Party,
        val verifier: Party,
        val accountName: String,
        val accountNumber: AccountNumber,
        @JsonSerialize(using = TokenTypeSerializer::class) val currency: TokenType,
        val type: BankAccountType,
        val verified: Boolean,
        override val linearId: UniqueIdentifier,
        val lastUpdated: Instant = Instant.now()
) : LinearState, QueryableState {

    constructor(
            owner: Party,
            verifier: Party,
            accountId: String,
            accountName: String,
            accountNumber: AccountNumber,
            currency: TokenType,
            type: BankAccountType
    ) : this(owner, verifier, accountName, accountNumber, currency, type, false, UniqueIdentifier(accountId))

    override val participants: List<AbstractParty> get() = listOf(owner)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is BankAccountStateSchemaV1 -> BankAccountStateSchemaV1.PersistentBankAccountState(
                    owner = owner.name.toString(),
                    verifier = verifier.name.toString(),
                    accountName = accountName,
                    accountNumber = accountNumber.digits,
                    currency = currency.tokenIdentifier,
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
