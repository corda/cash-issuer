package com.r3.corda.finance.cash.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.utilities.contextLogger

// TODO: Do we need to link commands to state groups? Or is there no point?
// TODO: Only require authorisation for transfers.
// TODO: Issuances can only be to the issuer. The issuer must then transfer the cash. (IssueAndTransfer).

class CashContract : Contract {

    companion object {
        @JvmStatic
        val CONTRACT_ID = "com.r3.corda.finance.cash.contracts.CashContract"
        private val logger = contextLogger()
    }

    override fun verify(tx: LedgerTransaction) {
        // The cash contract only cares about cash states and cash commands, it ignores everything else.
        // Cash transactions may only contain cash commands of one type per transaction! For example, it doesn't make
        // sense to transfer and issue cash in the same transaction for a multitude of reasons, therefore the contract
        // code doesn't allow you to do it.
        val cashCommands = tx.commandsOfType<CashContract.Commands>()
        val singleCashCommand = cashCommands.map { it.value }.toSet().singleOrNull()
        require(singleCashCommand != null) {
            "Transactions with cash states may only contain cash commands of one type."
        }

        // Extract all the signers. There might be more than one command but we now know they are all of the same type.
        val signers = cashCommands.flatMap { it.signers }.toSet()

//        when (singleCashCommand) {
//            is Issue -> {
//                verifyAuthorisation(tx)
//                verifyIssue(tx, signers)
//            }
//            is Move -> {
//                verifyAuthorisation(tx)
//
//            }
//            is Redeem -> { }
//        }
    }

//    /**
//     * All the public keys that are assigned ownership of cash states in issue and move transactions must be signed by
//     * the cash issuer. This is required to meet the EU's Anti-Money Laundering regulations.
//     * TODO Add some colour around regulations in other countries.
//     */
//    fun verifyAuthorisation(tx: LedgerTransaction) {
//        // Get all the cash output states from the transaction. We don't need to do anything special with the outputs.
//        // All outputs are treated equal in the eyes of the issuer - the owning keys are either signed or they are not.
//        val cashOutputs = tx.outputsOfType<CashState>()
//
//        // All cash states should have an 'expiresAt' time, before this time window.
//        val timeWindow = tx.timeWindow
//        require (timeWindow != null) {
//            "All cash transfers must include a time window."
//        }
//
//        // Use the issuer's legal entity public key, which is embedded in the cash state, to verify the signature over
//        // the new owner's public key. If the signature is invalid for _any_ of the cash output states, then the
//        // transaction containing them can never be valid. The implication is that if any party receiving cash output
//        // states in a transaction doesn't have a public key signed by the issuer then the transaction cannot be
//        // committed to the ledger. The remedy is to ask the issuer to sign some public keys and this can only be done
//        // if the requesting party is registered with the issuer of the cash states in question.
//        cashOutputs.forEach { (owner, authorisation, amount) ->
//            require (authorisation.authorisation.expiresAt < timeWindow!!.untilTime) {
//                "Cash authorisation expired!"
//            }
//
//            val issuer = amount.token.issuer
//            try {
//                Crypto.doVerify(
//                        publicKey = issuer.party.owningKey,
//                        signatureData = authorisation.signature.bytes,
//                        clearData = owner.owningKey.encoded
//                )
//            } catch (e: SignatureException) {
//                throw IllegalArgumentException("Invalid authorisation")
//            }
//        }
//    }
//
//    fun verifyIssue(tx: LedgerTransaction, signers: Set<PublicKey>) {
//        // Need to deal with issuer references.
//        // proably have to deal with multiple groups for issuances as there is a ref field in the Issued class.
//        val command = tx.commands.requireSingleCommand<Issue>()
//        requireThat {
//            "Cash issue transactions cannot contain any inputs." using tx.inputs.isEmpty()
//        }
//    }
//
//    fun verifyMove(tx: LedgerTransaction, signers: Set<PublicKey>) {
//        // Cash states are also split into groups which are keyed by issuer and currency pairs. This is because:
//        // - Different currencies are not fungible and need to be treated separately, e.g. a GBP/USD Fx transaction.
//        // - The same currency issued by different issuers cannot be considered fungible. This is because cash states
//        //   are actually commercial bank liabilities. A cash state gives the current owner (assuming a valid chain of
//        //   provenance) the right to redeem the specified amount of fiat currency from the issuing entity. Typically,
//        //   this fiat currency would held as a bank deposit. As such, cash states are subject to the credit risk of the
//        //   institution which holds the underlying deposit. Therefore it is not correct to say that £10 issued by Alice
//        //   is equivalent to £10 issued by Bob. Alice £s and Bob £s are NOT fungible and must be treated separately.
//        // - Different types of currency are not fungible. For example, central bank issued money is not equivalent to
//        //   commercial bank issued money of the same currency.
//        val cashGroups = tx.groupStates<CashState, Issued<Currency>> { cashState -> cashState.amount.token }
//
//        // The cash contract mandates that issues and redemptions may only involve one issuer per transaction and involve
//        // cash of one currency per transaction. As such, no state groups are required for issuances and redemptions.
//
//        cashGroups.forEach { (inputs, outputs, key) ->
//
//        }
//    }
//
//    fun verifyRedeem(tx: LedgerTransaction, signers: Set<PublicKey>) {
//
//    }

    interface Commands : CommandData
    class Issue : Commands, TypeOnlyCommandData()
    class Move : Commands, TypeOnlyCommandData()        // Need to assign move commands to each group.
    class Redeem : Commands, TypeOnlyCommandData()
}
