package com.allianz.t2i.common.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class NostroTransactionContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.allianz.t2i.common.contracts.NostroTransactionContract"
    }

    interface Commands : CommandData
    class Add : Commands
    class Match : Commands

    // TODO: Contract code not implemented for demo.
    override fun verify(tx: LedgerTransaction) = Unit

}