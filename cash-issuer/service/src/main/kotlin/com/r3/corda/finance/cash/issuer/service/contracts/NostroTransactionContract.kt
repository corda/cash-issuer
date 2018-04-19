package com.r3.corda.finance.cash.issuer.service.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class NostroTransactionContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.r3.corda.finance.cash.issuer.service.contracts.NostroTransactionContract"
    }

    interface Commands : CommandData
    class Add : Commands
    class Match : Commands

    override fun verify(tx: LedgerTransaction) {
//        val command = tx.commands.requireSingleCommand<Commands>()
//        val signers = command.signers.toSet()
//
//        when (command.value) {
//            is Add -> verifyAdd(tx, signers)
//            else -> throw IllegalArgumentException("Unrecognised command.")
//        }
    }

//    private fun verifyAdd(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
//        "This transaction cannot contain any input states." using (tx.inputs.isEmpty())
//        "This transaction must contain one or more output states." using (tx.outputs.isNotEmpty())
//        val areNostroTransactions = tx.outputStates.all { it is NostroTransactionState }
//        "All outputs must be Nostro Transaction States." using areNostroTransactions
//        // All outputs must have the same participant -- the issuer.
//        val verifySigners = signers.single() == tx.outputStates.flatMap { it.participants }.single().owningKey
//        "The issuer must only sign this transaction." using verifySigners
//    }

//    private fun verifyUpdate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
//        "This transaction must only contain one input state." using (tx.inputs.size == 1)
//        "This transaction must only contain one output state." using (tx.outputs.size == 1)
//        val input = tx.inputStates.single() as BankAccountState
//        val output = tx.outputStates.single() as BankAccountState
//        "Only the verified flag may change." using (input == output.copy(verified = input.verified))
//        "Last updated must change and be in the future." using (input.lastUpdated < output.lastUpdated)
//        val verifySigners = signers.single() == output.owner.owningKey
//        "Only the issuer must sign this transaction." using verifySigners
//    }

}