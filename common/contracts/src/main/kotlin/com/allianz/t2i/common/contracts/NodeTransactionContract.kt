package com.allianz.t2i.common.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class NodeTransactionContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.allianz.t2i.common.contracts.NodeTransactionContract"
    }

    interface Commands : CommandData
    class Create : Commands
    class Update : Commands

    // TODO: Contract code not implemented for demo.
    override fun verify(tx: LedgerTransaction) = Unit
}