package com.r3.bn

import com.r3.bn.common.types.MembershipStatus
import com.r3.bn.operator.flows.UpdateMembershipRecordStatus
import com.r3.bn.user.flows.JoinBusinessNetworkRequest
import com.r3.testutils.MockNetworkTest
import net.corda.node.internal.StartedNode
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.startFlow
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class UpdateMembershipRecordTests : MockNetworkTest(numberOfNodes = 1) {

    lateinit var I: StartedNode<MockNetwork.MockNode>
    lateinit var A: StartedNode<MockNetwork.MockNode>

    @Before
    override fun initialiseNodes() {
        I = nodes[0]
        A = nodes[1]
    }

    @Test
    fun `successfully update membership record and share with member`() {
        val issuer = I.info.legalIdentities.first()
        val party = A.info.legalIdentities.first()
        A.services.startFlow(JoinBusinessNetworkRequest(issuer))
        network.waitQuiescent()
        val result = I.services.startFlow(UpdateMembershipRecordStatus(party, MembershipStatus.APPROVED))
        network.waitQuiescent()
        val txI = result.resultFuture.get()
        val txA = A.services.validatedTransactions.getTransaction(txI.id)
        assertEquals(txI, txA)
    }

}