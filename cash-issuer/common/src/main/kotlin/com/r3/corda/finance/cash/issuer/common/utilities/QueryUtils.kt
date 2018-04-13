package com.r3.corda.finance.cash.issuer.common.utilities

import com.r3.corda.finance.cash.issuer.common.schemas.BankAccountStateSchemaV1
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.types.AccountNumber
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
    return getBankAccountState(services) {
        BankAccountStateSchemaV1.PersistentBankAccountState::accountNumber.equal(accountNumber.digits)
    }
}

fun getBankAccountStateByLinearId(linearId: UniqueIdentifier, services: ServiceHub): StateAndRef<BankAccountState>? {
    return getBankAccountState(services) {
        BankAccountStateSchemaV1.PersistentBankAccountState::linearId.equal(linearId.id.toString())
    }
}

private fun <T : PersistentState> getBankAccountState(services: ServiceHub, block: () -> CriteriaExpression<T, Boolean>): StateAndRef<BankAccountState>? {
    val query = builder {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val accountNumberCriteria = QueryCriteria.VaultCustomQueryCriteria(block())
        generalCriteria.and(accountNumberCriteria)
    }
    val result = services.vaultService.queryBy<BankAccountState>(query)
    return result.states.singleOrNull()
}