package com.allianz.t2i.common.workflows.utilities

import com.allianz.t2i.common.contracts.schemas.BankAccountStateSchemaV1
import com.allianz.t2i.common.contracts.schemas.NodeTransactionStateSchemaV1
import com.allianz.t2i.common.contracts.schemas.NostroTransactionStateSchemaV1
import com.allianz.t2i.common.contracts.states.BankAccountState
import com.allianz.t2i.common.contracts.states.NodeTransactionState
import com.allianz.t2i.common.contracts.states.NostroTransactionState
import com.allianz.t2i.common.contracts.types.AccountNumber
import com.allianz.t2i.common.contracts.types.NodeTransactionStatus
import com.allianz.t2i.common.contracts.types.NodeTransactionType
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder

/** Bunch of helpers for querying the vault. */

fun getBankAccountStateByAccountNumber(accountNumber: AccountNumber, services: ServiceHub): StateAndRef<BankAccountState>? {
    val states = getState<BankAccountState>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(BankAccountStateSchemaV1.PersistentBankAccountState::accountNumber.equal(accountNumber.digits))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}

fun getBankAccountStateByLinearId(linearId: UniqueIdentifier, services: ServiceHub): StateAndRef<BankAccountState>? {
    val states = getState<BankAccountState>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(BankAccountStateSchemaV1.PersistentBankAccountState::linearId.equal(linearId.id.toString()))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}

fun getNostroTransactionStateByTransactionId(transactionId: String, services: ServiceHub): StateAndRef<NostroTransactionState>? {
    val states = getState<NostroTransactionState>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(NostroTransactionStateSchemaV1.PersistentNostroTransactionState::transactionId.equal(transactionId))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}

fun getPendingRedemptionsByCounterparty(counterparty: String, services: ServiceHub): List<StateAndRef<NodeTransactionState>>? {
    return getState(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(NodeTransactionStateSchemaV1.PersistentNodeTransactionState::status.equal(NodeTransactionStatus.PENDING.name))
        val additionalCriteriaTwo = QueryCriteria.VaultCustomQueryCriteria(NodeTransactionStateSchemaV1.PersistentNodeTransactionState::type.equal(NodeTransactionType.REDEMPTION.name))
        val additionalCriteriaThree = QueryCriteria.VaultCustomQueryCriteria(NodeTransactionStateSchemaV1.PersistentNodeTransactionState::counterparty.equal(counterparty))
        generalCriteria.and(additionalCriteria.and(additionalCriteriaTwo.and(additionalCriteriaThree)))
    }
}

fun getPendingRedemptionByNotes(notes: String, services: ServiceHub): StateAndRef<NodeTransactionState>? {
    val states = getState<NodeTransactionState>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(NodeTransactionStateSchemaV1.PersistentNodeTransactionState::notes.equal(notes))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}

private inline fun <reified U : ContractState> getState(
        services: ServiceHub,
        block: (generalCriteria: QueryCriteria.VaultQueryCriteria) -> QueryCriteria
): List<StateAndRef<U>> {
    val query = builder {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        block(generalCriteria)
    }
    val result = services.vaultService.queryBy<U>(query)
    return result.states
}

fun getLatestNostroTransactionStatesGroupedByAccount(services: ServiceHub): Map<String, Long> {
    val query = builder {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val nostroTransactionCriteria = QueryCriteria.VaultCustomQueryCriteria(
                // Return transactions with the highest timestamp grouped by "accountId".
                NostroTransactionStateSchemaV1.PersistentNostroTransactionState::createdAt.max(
                        groupByColumns = listOf(NostroTransactionStateSchemaV1.PersistentNostroTransactionState::accountId)
                )
        )
        generalCriteria.and(nostroTransactionCriteria)
    }

    return services.vaultService.queryBy<NostroTransactionState>(query).otherResults.chunked(2).associate {
        it[1] as String to it[0] as Long
    }
}

fun getNostroAccountBalances(services: ServiceHub): Map<String, Long> {
    val query = builder {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val nostroTransactionCriteria = QueryCriteria.VaultCustomQueryCriteria(
                // Return transactions with the highest timestamp grouped by "accountId".
                NostroTransactionStateSchemaV1.PersistentNostroTransactionState::amount.sum(
                        groupByColumns = listOf(NostroTransactionStateSchemaV1.PersistentNostroTransactionState::accountId)
                )
        )
        generalCriteria.and(nostroTransactionCriteria)
    }

    return services.vaultService.queryBy<NostroTransactionState>(query).otherResults.chunked(2).associate {
        it[1] as String to it[0] as Long
    }
}

// TODO: Refactor this to use the above private function.
fun getNostroTransactionsByAccountNumber(accountNumber: AccountNumber, services: ServiceHub): List<StateAndRef<NostroTransactionState>> {
    val query = builder {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val sourceAccountNumber = QueryCriteria.VaultCustomQueryCriteria(
                NostroTransactionStateSchemaV1.PersistentNostroTransactionState::sourceAccountNumber.equal(accountNumber.digits)
        )
        val destinationAccountNumber = QueryCriteria.VaultCustomQueryCriteria(
                NostroTransactionStateSchemaV1.PersistentNostroTransactionState::destinationAccountNumber.equal(accountNumber.digits)
        )
        generalCriteria.and(sourceAccountNumber.or(destinationAccountNumber))
    }

    val result = services.vaultService.queryBy<NostroTransactionState>(query)
    return result.states
}

