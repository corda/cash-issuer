package com.r3.corda.finance.cash.states

import com.r3.corda.finance.cash.contracts.CashContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.Issued
import net.corda.core.contracts.OwnableState
import net.corda.core.identity.AbstractParty
import java.util.*

data class CashState(
        override val owner: AbstractParty,
        val amount: Amount<Issued<Currency>>
) : OwnableState {
    override val participants: List<AbstractParty> get() = listOf(owner)
    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(CashContract.Move(), copy(owner = newOwner))
    }
}