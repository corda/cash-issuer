package test

import com.r3.corda.finance.cash.issuer.client.flows.RedeemCash
import com.r3.corda.finance.cash.issuer.service.flows.AddNostroTransactions
import com.r3.corda.sdk.issuer.common.contracts.states.BankAccountState
import com.r3.corda.sdk.issuer.common.contracts.states.NodeTransactionState
import com.r3.corda.sdk.issuer.common.contracts.states.NostroTransactionState
import com.r3.corda.sdk.issuer.common.contracts.types.BankAccount
import com.r3.corda.sdk.issuer.common.contracts.types.BankAccountType
import com.r3.corda.sdk.issuer.common.contracts.types.NostroTransaction
import com.r3.corda.sdk.issuer.common.contracts.types.UKAccountNumber
import com.r3.corda.sdk.issuer.common.workflows.flows.AddBankAccount
import com.r3.corda.sdk.issuer.common.workflows.flows.MoveCash
import com.r3.corda.sdk.token.contracts.states.FungibleToken
import com.r3.corda.sdk.token.money.GBP
import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.toFuture
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import org.junit.Test
import rx.Observable
import rx.schedulers.Schedulers
import java.time.Instant
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals

class IntegrationTest {

    companion object {
        private val log = contextLogger()
    }

    private val partyA = NodeParameters(
            providedName = CordaX500Name("PartyA", "London", "GB"),
            additionalCordapps = listOf(TestCordapp.findCordapp("com.r3.corda.finance.cash.issuer.client"))
    )

    private val partyB = NodeParameters(
            providedName = CordaX500Name("PartyB", "London", "GB"),
            additionalCordapps = listOf(TestCordapp.findCordapp("com.r3.corda.finance.cash.issuer.client"))
    )

    private val issuer = NodeParameters(
            providedName = CordaX500Name("Issuer", "London", "GB"),
            additionalCordapps = listOf(TestCordapp.findCordapp("com.r3.corda.finance.cash.issuer.service"))
    )

    private val defaultCorDapps = listOf(
            TestCordapp.findCordapp("com.r3.corda.sdk.issuer.common.contracts"),
            TestCordapp.findCordapp("com.r3.corda.sdk.issuer.common.workflows"),
            TestCordapp.findCordapp("com.r3.corda.sdk.token.workflow"),
            TestCordapp.findCordapp("com.r3.corda.sdk.token.contracts"),
            TestCordapp.findCordapp("com.r3.corda.sdk.token.money")
    )

    private val driverParameters = DriverParameters(
            startNodesInProcess = true,
            cordappsForAllNodes = defaultCorDapps,
            networkParameters = testNetworkParameters(notaries = emptyList(), minimumPlatformVersion = 4)
    )

    @Test
    fun `node test`() {
        driver(driverParameters) {
            val A = startNode(partyA).getOrThrow()
            val B = startNode(partyB).getOrThrow()
            val I = startNode(issuer).getOrThrow()

            log.info("All nodes started up.")

            val issuerBankAccount = BankAccount(
                    accountId = "1",
                    accountName = "Issuer Collateral Account",
                    accountNumber = UKAccountNumber(sortCode = "224466", accountNumber = "11228899"),
                    currency = GBP,
                    type = BankAccountType.COLLATERAL
            )

            val partyABankAccount = BankAccount(
                    accountId = "2",
                    accountName = "Party A Bank Account",
                    accountNumber = UKAccountNumber(sortCode = "112233", accountNumber = "44557788"),
                    currency = GBP,
                    type = BankAccountType.COLLATERAL
            )

            val partyBBankAccount = BankAccount(
                    accountId = "3",
                    accountName = "Party B Bank Account",
                    accountNumber = UKAccountNumber(sortCode = "996633", accountNumber = "11663300"),
                    currency = GBP,
                    type = BankAccountType.COLLATERAL
            )

            val paymentToIssuerNostro = NostroTransaction(
                    transactionId = "1",
                    accountId = "1",
                    amount = 1000L,
                    currency = GBP,
                    type = "",
                    description = "",
                    createdAt = Instant.now(),
                    source = UKAccountNumber(sortCode = "112233", accountNumber = "44557788"),      // Party A.
                    destination = UKAccountNumber(sortCode = "224466", accountNumber = "11228899")
            )

            fun generatePaymentFromIssuerNostro(secretCode: String): NostroTransaction {
                return NostroTransaction(
                        transactionId = "2",
                        accountId = "1",
                        amount = -200L,
                        currency = GBP,
                        type = "",
                        description = secretCode,
                        createdAt = Instant.now(),
                        source = UKAccountNumber(sortCode = "224466", accountNumber = "11228899"),
                        destination = UKAccountNumber(sortCode = "996633", accountNumber = "11663300")  // Party B.
                )
            }

            val issuerParty = I.nodeInfo.legalIdentities.first()
            val aParty = A.nodeInfo.legalIdentities.first()
            val bParty = B.nodeInfo.legalIdentities.first()

            // -----------------------------
            // Stage 1 - Add issuer account.
            // -----------------------------

            // Start add bank account flow.
            val addIssuerBankAccountFlow = I.rpc.startFlow(::AddBankAccount, issuerBankAccount, issuerParty).returnValue.toCompletableFuture()
            // Confirm state is stored in the vault of the issuer.
            val stageOneIssuerVaultUpdate = I.rpc.vaultTrack(BankAccountState::class.java).updates.toFuture().toCompletableFuture()
            // Wait for all futures to complete.
            CompletableFuture.allOf(addIssuerBankAccountFlow, stageOneIssuerVaultUpdate)
            // Print transaction.
            println("Issuer bank account state added.")
            println(addIssuerBankAccountFlow.get().tx)
            // TODO: Need to manually verify bank account.

            // ------------------------------
            // Stage 2 - Add Party A account.
            // ------------------------------

            // Add partyA account and check it was added.
            val addPartyABankAccountFlow = A.rpc.startFlow(::AddBankAccount, partyABankAccount, issuerParty).returnValue.toCompletableFuture()
            // Confirm state is stored in the vault of the issuer and party A.
            val stageTwoIssuerVaultUpdate = I.rpc.vaultTrack(BankAccountState::class.java).updates.toFuture().toCompletableFuture()
            val stageTwoPartyAVaultUpdate = A.rpc.vaultTrack(BankAccountState::class.java).updates.toFuture().toCompletableFuture()
            // Wait for all futures to complete.
            CompletableFuture.allOf(stageTwoIssuerVaultUpdate, stageTwoPartyAVaultUpdate, addPartyABankAccountFlow)
            // Check the issuer and party A have the same state.
            assertEquals(stageTwoIssuerVaultUpdate.get().produced.single(), stageTwoPartyAVaultUpdate.get().produced.single())
            // Print transaction.
            println("Party A bank account state added.")
            // Confirm that the bank account verification was done.
            val stageTwoIssuerVerifyBankAccountStateUpdate = I.rpc.vaultTrack(BankAccountState::class.java).updates.toFuture().toCompletableFuture()
            println(addPartyABankAccountFlow.get().tx)
            println("Party A bank account verified.")
            val verifyBankAccountStateVaultUpdate = stageTwoIssuerVerifyBankAccountStateUpdate.getOrThrow()
            //assertEquals(verifyBankAccountStateVaultUpdate.consumed.single(), stageTwoIssuerVaultUpdate.get().produced.single())
            println(verifyBankAccountStateVaultUpdate)

            // ---------------------------------
            // Stage 3 - Add nostro transaction.
            // ---------------------------------

            // The updates the internal state of the issuer and issues the same amount of currency (as tokens) that was
            // "paid" into the issuer's bank account.

            println("Add nostro transaction (issue cash)")
            val addNostroTransactionFlow = I.rpc.startFlow(::AddNostroTransactions, listOf(paymentToIssuerNostro)).returnValue.toCompletableFuture()
            // Get the first nostro transaction state added to the database.
            val newNostroTransactionState = I.rpc.vaultTrack(NostroTransactionState::class.java).updates.getOrThrow()
            println(newNostroTransactionState)
            // Get the updated nostro transaction state (MATCHED) and node transaction state.
            val nostroTransactionStateUpdate = I.rpc.vaultTrack(ContractState::class.java).updates.getOrThrow()
            println(nostroTransactionStateUpdate)
            // Get the updated node transaction state (COMPLETE).
            val nostroTransactionStateUpdateTwo = I.rpc.vaultTrack(NodeTransactionState::class.java).updates.getOrThrow()
            println(nostroTransactionStateUpdateTwo)
            // Get the cash issuance.
            val newCashOnPartyA = A.rpc.vaultTrack(FungibleToken::class.java).updates.getOrThrow()
            println(newCashOnPartyA)
            addNostroTransactionFlow.getOrThrow()

            // ----------------------
            // Stage 4 - Cash payment.
            // ----------------------

            println("Cash payment.")
            val moveCashFlow = A.rpc.startFlowDynamic(MoveCash::class.java, bParty, 500.GBP).returnValue.toCompletableFuture()
            val newTokenMoveA = A.rpc.vaultTrack(FungibleToken::class.java).updates.toFuture().toCompletableFuture()
            val newTokenMoveB = B.rpc.vaultTrack(FungibleToken::class.java).updates.toFuture().toCompletableFuture()
            CompletableFuture.allOf(moveCashFlow, newTokenMoveA, newTokenMoveB)
            println(moveCashFlow.getOrThrow().tx)

            // -------------------------------------
            // Stage 5 - Add Party B's bank account.
            // -------------------------------------

            // Start add bank account flow.
            val addPartyBBankAccountFlow = B.rpc.startFlow(::AddBankAccount, partyBBankAccount, issuerParty).returnValue.toCompletableFuture()
            // Confirm state is stored in the vault of the issuer.
            val stageFiveIssuerVaultUpdate = I.rpc.vaultTrack(BankAccountState::class.java).updates.toFuture().toCompletableFuture()
            // Wait for all futures to complete.
            CompletableFuture.allOf(addPartyBBankAccountFlow, stageFiveIssuerVaultUpdate)
            // Print transaction.
            println("Party B bank account state added.")
            println(addPartyBBankAccountFlow.get().tx)

            // ------------------------------------
            // Stage 6 - Redeem tokens with change.
            // ------------------------------------

            val redeemTx = B.rpc.startFlowDynamic(RedeemCash::class.java, 200.GBP, issuerParty).returnValue.toCompletableFuture()
            val partyBchange = B.rpc.vaultTrack(FungibleToken::class.java).updates.toFuture().toCompletableFuture()
            CompletableFuture.allOf(redeemTx, partyBchange)
            println("Party B change:")
            println(partyBchange.getOrThrow())
            println(redeemTx.getOrThrow().tx)
            val nodeTxStateTracker = I.rpc.vaultTrack(NodeTransactionState::class.java).updates.toFuture().toCompletableFuture()
            // Generate the payment from the issuer to the redeeming party's bank account.
            val nodeTxState = nodeTxStateTracker.getOrThrow()
            val secretCode = nodeTxState.produced.single().state.data.notes
            val nostroTx = generatePaymentFromIssuerNostro(secretCode)
            val redeemNostroTxFlow = I.rpc.startFlow(::AddNostroTransactions, listOf(nostroTx)).returnValue.toCompletableFuture()
            // Get the first nostro transaction state added to the database.
            val redeemNostroTxState = I.rpc.vaultTrack(NostroTransactionState::class.java).updates.getOrThrow()
            println(redeemNostroTxState)
            val updateNostroTxState = I.rpc.vaultTrack(NostroTransactionState::class.java).updates.getOrThrow()
            println(updateNostroTxState)
            val nodeTxStateTrackerTwo = I.rpc.vaultTrack(NodeTransactionState::class.java).updates.getOrThrow()
            println(nodeTxStateTrackerTwo)
            redeemNostroTxFlow.getOrThrow()
        }

    }
}

fun <T : Any> Observable<T>.getOrThrow() = observeOn(Schedulers.io())
        .toFuture()
        .getOrThrow()