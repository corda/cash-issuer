package com.r3.corda.finance.cash.issuer.common.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class NodeTransactionContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.r3.corda.finance.cash.issuer.common.contracts.NodeTransactionContract"
    }

    interface Commands : CommandData
    class Create : Commands
    class Update : Commands

    override fun verify(tx: LedgerTransaction) {

    }
}