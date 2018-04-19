package com.r3.corda.finance.cash.issuer.service.contracts

import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class NodeTransactionContract : Contract {
    override fun verify(tx: LedgerTransaction) {

    }
}