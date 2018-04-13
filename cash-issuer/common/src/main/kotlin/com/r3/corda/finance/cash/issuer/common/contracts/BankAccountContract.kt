package com.r3.corda.finance.cash.issuer.common.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class BankAccountContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.r3.corda.finance.cash.issuer.common.contracts.BankAccountContract"
    }

    interface Commands : CommandData
    class Add : Commands

    override fun verify(tx: LedgerTransaction) = Unit

}