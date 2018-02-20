package com.r3.cash.issuer.old

import com.r3.cash.issuer.daemon.GenerationScheme
import com.r3.cash.issuer.daemon.generateRandomString
import com.r3.cash.issuer.daemon.mockbank.MockBank
import com.r3.cash.issuer.daemon.monzo.MockMonzoBank
import com.r3.cash.issuer.daemon.monzo.MockMonzoBankAccount
import net.corda.core.identity.CordaX500Name
import net.corda.node.internal.StartedNode
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before

abstract class MockNetworkTestOld(private val numberOfNodes: Int = 2, numberOfBanks: Int = 1) {

    lateinit protected var network: MockNetwork
    lateinit protected var nodes: List<StartedNode<MockNetwork.MockNode>>
    protected var banks: List<MockBank> = createBanksAndAccounts(numberOfBanks)

    @Before
    fun setup() {
        network = MockNetwork(cordappPackages = listOf("com.r3.cash.issuer", "net.corda.finance"), threadPerNode = true)
        val issuer = network.createPartyNode(CordaX500Name("Issuer", "London", "GB"))
        nodes = createSomeNodes(numberOfNodes) + issuer
    }

    @After
    fun shutdown() {
        network.stopNodes()
    }

    abstract fun initialiseNodes()

    /**
     * Creates n number of banks each with one bank account for the issuer.
     */
    private fun createBanksAndAccounts(numberOfBanks: Int): List<MockMonzoBank> {
        return (1..numberOfBanks).map {
            val bank = MockMonzoBank()
            val randomAccountNumber = generateRandomString(8, GenerationScheme.NUMBERS)
            val account = bank.openAccount("R3 Cash Issuer Ltd", randomAccountNumber) as MockMonzoBankAccount
            account.startGeneratingTransactions()
            bank
        }
    }

    private fun createSomeNodes(numberOfNodes: Int): List<StartedNode<MockNetwork.MockNode>> {
        return (1..numberOfNodes).map { current ->
            val char = current.toChar() + 64
            val name = CordaX500Name("Party$char", "London", "GB")
            network.createPartyNode(name)
        }
    }

}