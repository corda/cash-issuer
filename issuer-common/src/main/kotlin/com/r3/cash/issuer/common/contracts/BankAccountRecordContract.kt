package com.r3.cash.issuer.common.contracts

import com.r3.cash.issuer.common.commands.CashIssuerAdministrationCommand
import com.r3.cash.issuer.common.commands.Create
import com.r3.cash.issuer.common.commands.Delete
import com.r3.cash.issuer.common.commands.Update
import com.r3.cash.issuer.common.states.BankAccountRecord
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.security.PublicKey

class BankAccountRecordContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.r3.cash.issuer.common.contracts.BankAccountRecordContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<CashIssuerAdministrationCommand>()
        val signers = command.signers.toSet()

        when (command.value) {
            is Create -> verifyCreate(tx, signers)
            is Update -> verifyUpdate(tx, signers)
            is Delete -> verifyDelete(tx, signers)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "This transaction cannot contain any input states." using (tx.inputs.isEmpty())
        "This transaction must only contain one output state." using (tx.outputs.size == 1)
        val output = tx.outputStates.single() as BankAccountRecord
        val matchedSigners = signers.toSet() == setOf(output.issuer.owningKey, output.accountOwner.owningKey)
        "The account owner only must sign this transaction." using matchedSigners
    }

    private fun verifyUpdate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "This transaction must only contain one input state." using (tx.inputs.size == 1)
        "This transaction must only contain one output state." using (tx.outputs.size == 1)
        val input = tx.inputStates.single() as BankAccountRecord
        val output = tx.outputStates.single() as BankAccountRecord
        val restrictChange = output.copy(accountName = input.accountName, accountNumber = input.accountNumber, sortCode = input.sortCode) == input
        "Only the account name, number and sort code can change in an update transaction." using restrictChange
        // Sort code and account name may remain the same.
        "The account number must change." using (input.accountNumber != output.accountNumber)
        val matchedSigners = signers.single() == output.accountOwner.owningKey
        "The account owner only must sign this transaction." using matchedSigners
    }

    private fun verifyDelete(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "This transaction must only contain one input state." using (tx.inputs.size == 1)
        "This transaction cannot contain any output states." using (tx.outputs.isEmpty())
        val input = tx.inputStates.single() as BankAccountRecord
        val matchedSigners = signers.single() == input.accountOwner.owningKey
        "The account owner only must sign this transaction." using matchedSigners
    }

}