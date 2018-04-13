package com.r3.corda.finance.cash.issuer.common.utilities

import com.r3.corda.finance.cash.issuer.common.schemas.BankAccountStateSchemaV1
import com.r3.corda.finance.cash.issuer.common.schemas.NostroTransactionStateSchemaV1
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.states.NostroTransactionState
import com.r3.corda.finance.cash.issuer.common.types.AccountNumber
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.CriteriaExpression
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.schemas.PersistentState

fun getBankAccountStateByAccountNumber(accountNumber: AccountNumber, services: ServiceHub): StateAndRef<BankAccountState>? {
    return getState(services) {
        BankAccountStateSchemaV1.PersistentBankAccountState::accountNumber.equal(accountNumber.digits)
    }
}

fun getBankAccountStateByLinearId(linearId: UniqueIdentifier, services: ServiceHub): StateAndRef<BankAccountState>? {
    return getState(services) {
        BankAccountStateSchemaV1.PersistentBankAccountState::linearId.equal(linearId.id.toString())
    }
}

fun getNostroTransactionStateByTransactionId(transactionId: String, services: ServiceHub): StateAndRef<NostroTransactionState>? {
    return getState(services) {
        NostroTransactionStateSchemaV1.PersistentNostroTransactionState::transactionId.equal(transactionId)
    }
}

private inline fun <T : PersistentState, reified U : ContractState> getState(services: ServiceHub, block: () -> CriteriaExpression<T, Boolean>): StateAndRef<U>? {
    val query = builder {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val customCriteria = QueryCriteria.VaultCustomQueryCriteria(block())
        generalCriteria.and(customCriteria)
    }
    val result = services.vaultService.queryBy<U>(query)
    return result.states.singleOrNull()
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

