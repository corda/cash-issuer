package com.r3.bn.common.contracts

import com.r3.bn.common.commands.BusinessNetworkAdministrationCommand
import com.r3.bn.common.commands.Create
import com.r3.bn.common.commands.Update
import com.r3.bn.common.states.MembershipRecord
import com.r3.bn.common.types.MembershipStatus
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.security.PublicKey

class MembershipRecordContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.r3.bn.common.contracts.MembershipRecordContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<BusinessNetworkAdministrationCommand>()
        val signers = command.signers.toSet()

        when (command.value) {
            is Create -> verifyRequest(tx, signers)
            is Update -> verifyUpdate(tx, signers)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyRequest(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "This transaction cannot contain any input states." using (tx.inputs.isEmpty())
        "This transaction must only contain one output state." using (tx.outputs.size == 1)
        val output = tx.outputStates.single() as MembershipRecord
        "Membership record status must be REQUESTED." using (output.status == MembershipStatus.REQUESTED)
        val matchedSigners = signers.toSet() == setOf(output.operator.owningKey, output.member.owningKey)
        "The BNO and the node requesting to join the BN must sign this transaction." using matchedSigners
    }

    private fun verifyUpdate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "This transaction must only contain one input state." using (tx.inputs.size == 1)
        "This transaction must only contain one output state." using (tx.outputs.size == 1)
        val input = tx.inputStates.single() as MembershipRecord
        val output = tx.outputStates.single() as MembershipRecord
        "Only the status can change in an update transaction." using (output.copy(status = input.status) == input)
        "The status must change." using (input.status != output.status)
        val matchedSigners = signers.single() == output.operator.owningKey
        "Only the BNO must sign an update transaction." using matchedSigners
        "The status transaction must be valid." using validStatusTransitions.contains(Pair(input.status, output.status))
    }

    private val validStatusTransitions = setOf(
            Pair(MembershipStatus.REQUESTED, MembershipStatus.APPROVED),
            Pair(MembershipStatus.REQUESTED, MembershipStatus.REJECTED),
            Pair(MembershipStatus.REJECTED, MembershipStatus.APPROVED),
            Pair(MembershipStatus.APPROVED, MembershipStatus.SUSPENDED),
            Pair(MembershipStatus.SUSPENDED, MembershipStatus.APPROVED)
    )

}