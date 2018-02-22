package com.r3.cash.issuer.common.states

import com.r3.cash.issuer.common.commands.Create
import com.r3.cash.issuer.common.commands.Delete
import com.r3.cash.issuer.common.commands.Update
import com.r3.cash.issuer.common.contracts.BankAccountRecordContract
import com.r3.cash.issuer.common.schemas.BankAccountRecordSchemaV1
import com.r3.cash.issuer.common.types.AccountNumber
import com.r3.cash.issuer.common.types.SortCode
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

data class BankAccountRecord(
        val accountOwner: Party,
        val issuer: Party,
        val accountName: String,
        val accountNumber: AccountNumber,
        val sortCode: SortCode,
        val verified: Boolean = false,
        val lastUpdated: Instant = Instant.now(),
        override val participants: List<AbstractParty> = listOf(accountOwner, issuer),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    companion object {
        fun createNewBankAccountRecord(
                accountOwner: Party,
                issuer: Party,
                accountName: String,
                accountNumber: AccountNumber,
                sortCode: SortCode,
                services: ServiceHub
        ): TransactionBuilder {
            // TODO: We need to revisit notary selection for production networks.
            val notary = services.networkMapCache.notaryIdentities.first()
            val command = Command(Create(), listOf(issuer.owningKey, accountOwner.owningKey))
            val newRequest = BankAccountRecord(accountOwner, issuer, accountName, accountNumber, sortCode)
            val stateAndContract = StateAndContract(newRequest, BankAccountRecordContract.CONTRACT_ID)
            return TransactionBuilder(notary = notary).withItems(command, stateAndContract)
        }

        fun updateBankAccountRecord(
                currentRecord: StateAndRef<BankAccountRecord>,
                newAccountName: String,
                newAccountNumber: AccountNumber,
                newSortCode: SortCode
        ): TransactionBuilder {
            val notary = currentRecord.state.notary
            val inputState = currentRecord.state.data
            val command = Command(Update(), listOf(inputState.accountOwner.owningKey))
            val outputState = inputState.updateAccountDetails(newAccountName, newAccountNumber, newSortCode)
            val stateAndContract = StateAndContract(outputState, BankAccountRecordContract.CONTRACT_ID)
            return TransactionBuilder(notary = notary).withItems(command, stateAndContract, currentRecord)
        }

        fun deleteBankAccountRecord(
                currentRecord: StateAndRef<BankAccountRecord>,
                newAccountName: String,
                newAccountNumber: AccountNumber,
                newSortCode: SortCode
        ): TransactionBuilder {
            val notary = currentRecord.state.notary
            val inputState = currentRecord.state.data
            val command = Command(Delete(), listOf(inputState.accountOwner.owningKey))
            return TransactionBuilder(notary = notary).withItems(command, currentRecord)
        }
    }

    fun updateAccountDetails(newAccountName: String, newAccountNumber: AccountNumber, newSortCode: SortCode): BankAccountRecord {
        return this.copy(accountName = newAccountName, accountNumber = newAccountNumber, sortCode = newSortCode)
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is BankAccountRecordSchemaV1 -> BankAccountRecordSchemaV1.PersistentBankAccountRecord(
                    this.accountOwner.name.toString(),
                    this.issuer.name.toString(),
                    this.accountName,
                    this.accountNumber.digits,
                    this.sortCode.digits,
                    this.verified,
                    this.lastUpdated,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BankAccountRecordSchemaV1)
}