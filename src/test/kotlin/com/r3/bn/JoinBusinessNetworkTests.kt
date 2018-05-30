package com.r3.bn

import com.r3.bn.user.flows.JoinBusinessNetworkRequest
import com.r3.testutils.MockNetworkTest
import net.corda.node.internal.StartedNode
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.startFlow
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class JoinBusinessNetworkTests : MockNetworkTest(numberOfNodes = 1) {

    lateinit var I: StartedNode<MockNetwork.MockNode>
    lateinit var A: StartedNode<MockNetwork.MockNode>

    @Before
    override fun initialiseNodes() {
        I = nodes[0]
        A = nodes[1]
    }

    @Test
    fun `successfully send request to join business network`() {
        val issuer = I.info.legalIdentities.first()
        val joinBnRequest = JoinBusinessNetworkRequest(issuer)
        val result = A.services.startFlow(joinBnRequest)
        I.smm.changes.subscribe { println(it) }
        network.waitQuiescent()

        val transactionA = result.resultFuture.get()
        val transactionI = I.services.validatedTransactions.getTransaction(transactionA.id)
        assertEquals(transactionA, transactionI)
    }

    @Test
    fun `Trying to join business network twice fails`() {
        val issuer = I.info.legalIdentities.first()
        A.services.startFlow(JoinBusinessNetworkRequest(issuer))
        network.waitQuiescent()
        // TODO: Figure out how to assert an exception.
        A.services.startFlow(JoinBusinessNetworkRequest(issuer))
        network.waitQuiescent()
    }

}