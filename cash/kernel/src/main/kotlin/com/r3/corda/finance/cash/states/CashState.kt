package com.r3.corda.finance.cash.states

import net.corda.core.contracts.Amount
import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.PartyAndReference
import net.corda.core.identity.AbstractParty
import java.util.*

data class CashState(
        override val owner: AbstractParty,
        val amount: Amount<Currency>,
        val issuer: PartyAndReference
) : OwnableState {
    override val participants: List<AbstractParty>
        get() = listOf(owner)

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {

    }
}