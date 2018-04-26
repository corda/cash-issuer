package com.r3.corda.finance

import com.r3.corda.finance.cash.issuer.client.flows.RedeemCash
import com.r3.corda.finance.cash.issuer.client.flows.SendBankAccount
import com.r3.corda.finance.cash.issuer.common.flows.AddBankAccount
import com.r3.corda.finance.cash.issuer.common.flows.MoveCash
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.r3.corda.finance.cash.issuer.common.types.NostroTransaction
import com.r3.corda.finance.cash.issuer.common.types.UKAccountNumber
import com.r3.corda.finance.cash.issuer.service.flows.AddNostroTransactions
import com.r3.corda.finance.cash.issuer.service.flows.IssueCashInternal
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.toStringShort
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.finance.GBP
import net.corda.testing.node.StartedMockNode
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.*

class IssueMoveAndRedeemCash : MockNetworkTest(numberOfNodes = 2) {

    lateinit var I: StartedMockNode
    lateinit var A: StartedMockNode
    lateinit var B: StartedMockNode

    @Before
    override fun initialiseNodes() {
        I = nodes[0]
        A = nodes[1]
        B = nodes[2]
    }

    private fun StartedMockNode.identity(): Party {
        return this.info.legalIdentities.first()
    }

    private fun StartedMockNode.createNostroTransaction(amount: Long, description: String, from: String) {
        val nTx = NostroTransaction(
                transactionId = "abc",
                accountId = identity().owningKey.toStringShort(), // one account for now.
                amount = amount,
                currency = GBP,
                type = "TYPE",
                description = "",
                createdAt = Instant.now(),
                source = UKAccountNumber(from),
                destination = UKAccountNumber("11111122224444")
        )
        startFlow(AddNostroTransactions(listOf(nTx))).getOrThrow()
    }

    private fun issueCash(to: Party, amount: Amount<Currency>): SignedTransaction {
        return I.startFlow(IssueCashInternal(to = to, amount = amount)).getOrThrow()
    }

    private fun StartedMockNode.moveCash(to: Party, amount: Amount<Currency>): SignedTransaction {
        return startFlow(MoveCash(recipient = to, amount = amount)).getOrThrow()
    }

    private fun StartedMockNode.redeemCash(issuer: Party, amount: Amount<Currency>) {
        startFlow(RedeemCash(issuer = issuer, amount = amount)).getOrThrow()
    }

    private fun StartedMockNode.addBankAccount(digits: String): UniqueIdentifier {
        val bankAccount = BankAccount(
                accountId = identity().owningKey.toStringShort(),
                accountName = "${identity().name.organisation}'s account",
                accountNumber = UKAccountNumber(digits),
                currency = GBP
        )
        return startFlow(AddBankAccount(bankAccount)).getOrThrow().tx.outputsOfType<BankAccountState>().single().linearId
    }

    private fun StartedMockNode.sendBankAccount(issuer: Party, linearId: UniqueIdentifier) {
        startFlow(SendBankAccount(issuer, linearId))
    }

    @Test
    fun `issue move then redeem`() {
        val aBankAccountId = A.addBankAccount("12345612345678")
        val bBankAccountId = B.addBankAccount("24681012141618")
        I.addBankAccount("11111122224444")
        A.sendBankAccount(I.identity(), aBankAccountId)
        B.sendBankAccount(I.identity(), bBankAccountId)
        // Wait for all the bank account stuff to finish...
        network.waitQuiescent()
        I.createNostroTransaction(50L, "Test transaction", "12345612345678")
//        issueCash(A.identity(), 50.POUNDS)
//        issueCash(A.identity(), 1.POUNDS)
//        issueCash(A.identity(), 1.POUNDS)
//        issueCash(A.identity(), 1.POUNDS)
//        issueCash(A.identity(), 10.POUNDS)
//        A.moveCash(B.identity(), 40.POUNDS)
//        B.redeemCash(I.identity(), 20.POUNDS)
        // val nTx = NostroTransactionState()
        A.services.validatedTransactions.updates.toBlocking().subscribe {
            println(it)
        }
    }
}