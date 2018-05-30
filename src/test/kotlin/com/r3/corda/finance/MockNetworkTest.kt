package com.r3.corda.finance

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before

abstract class MockNetworkTest(private val numberOfNodes: Int = 2) {

    lateinit protected var network: MockNetwork
    lateinit protected var nodes: List<StartedMockNode>

    @Before
    fun setup() {
        val corDappPackages = listOf(
                "net.corda.finance",
                "com.r3.corda.finance.cash.issuer.common",
                "com.r3.corda.finance.cash.issuer.service",
                "com.r3.corda.finance.cash.issuer.client"
        )

        network = MockNetwork(
                cordappPackages = corDappPackages,
                threadPerNode = true,
                notarySpecs = listOf(MockNetworkNotarySpec(name = DUMMY_NOTARY_NAME, validating = false))
        )
        // Always create an issuer which should be accessible via index zero in the list of nodes.
        val issuer = network.createPartyNode(CordaX500Name("Issuer", "London", "GB"))
        nodes = listOf(issuer) + createSomeNodes(numberOfNodes)
    }

    @After
    fun shutdown() {
        network.stopNodes()
    }

    abstract fun initialiseNodes()

    private fun createSomeNodes(numberOfNodes: Int): List<StartedMockNode> {
        return (1..numberOfNodes).map { current ->
            val char = current.toChar() + 64
            val name = CordaX500Name("Party$char", "London", "GB")
            network.createNode(legalName = name, cordappPackagesToExclude = listOf("com.r3.corda.finance.cash.issuer.service"))
        }
    }

}