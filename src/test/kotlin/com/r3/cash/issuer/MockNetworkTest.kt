package com.r3.cash.issuer

import net.corda.core.identity.CordaX500Name
import net.corda.node.internal.StartedNode
import net.corda.testing.node.MockNetwork
import org.junit.After
import org.junit.Before

abstract class MockNetworkTest(private val numberOfNodes: Int = 2) {

    lateinit protected var network: MockNetwork
    lateinit protected var nodes: List<StartedNode<MockNetwork.MockNode>>

    @Before
    fun setup() {
        val corDappPackages = listOf(
                "com.r3.cash.issuer",
                "com.r3.cash.issuer.base",
                "com.r3.cash.issuer.client",
                "com.r3.cash.issuer.daemon",
                "net.corda.finance"
        )

        network = MockNetwork(cordappPackages = corDappPackages, threadPerNode = true)
        // Always create an issuer which should be accessible via index zero in the list of nodes.
        val issuer = network.createPartyNode(CordaX500Name("Issuer", "London", "GB"))
        nodes = listOf(issuer) + createSomeNodes(numberOfNodes)
    }

    @After
    fun shutdown() {
        network.stopNodes()
    }

    abstract fun initialiseNodes()

    private fun createSomeNodes(numberOfNodes: Int): List<StartedNode<MockNetwork.MockNode>> {
        return (1..numberOfNodes).map { current ->
            val char = current.toChar() + 64
            val name = CordaX500Name("Party$char", "London", "GB")
            network.createPartyNode(name)
        }
    }

}