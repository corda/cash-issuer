package com.r3.cash.issuer

import com.r3.cash.issuer.base.types.RecordStatus
import com.r3.cash.issuer.base.types.RequestToJoinBusinessNetworkResponse
import com.r3.cash.issuer.client.flows.RequestToJoin
import com.r3.cash.issuer.client.flows.UpdateAccountInfo
import com.r3.cash.issuer.flows.ListAllAccounts
import com.r3.cash.issuer.flows.UpdateRecordStatus
import net.corda.node.internal.StartedNode
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.startFlow
import org.junit.Before
import org.junit.Test

class NetworkMembershipTests : MockNetworkTest(numberOfNodes = 1) {

    lateinit var ISSUER: StartedNode<MockNetwork.MockNode>
    lateinit var A: StartedNode<MockNetwork.MockNode>

    @Before
    override fun initialiseNodes() {
        ISSUER = nodes[0]
        A = nodes[1]
    }

    @Test
    fun `request to join the cash business network twice fails`() {
        val issuerIdentity = ISSUER.info.legalIdentities.first()
        A.services.startFlow(RequestToJoin(issuerIdentity))
        network.waitQuiescent()

        val result = A.services.startFlow(RequestToJoin(issuerIdentity)).resultFuture.get()
        network.waitQuiescent()
        assert(result is RequestToJoinBusinessNetworkResponse.Failure)
    }

    @Test
    fun `join then get approved then add a bank account`() {
        val issuerIdentity = ISSUER.info.legalIdentities.first()
        val partyAIdentity = A.info.legalIdentities.first()
        A.services.startFlow(RequestToJoin(issuerIdentity))
        network.waitQuiescent()
        ISSUER.services.startFlow(UpdateRecordStatus(partyAIdentity, RecordStatus.APPROVED))
        network.waitQuiescent()
        val updateAccountInfoFlow = UpdateAccountInfo(
                issuer = issuerIdentity,
                accountName = "A Ltd",
                accountNumber = "12345678",
                sortCode = "123456"
        )
        A.services.startFlow(updateAccountInfoFlow)
        network.waitQuiescent()
        ISSUER.services.startFlow(ListAllAccounts())
        network.waitQuiescent()
    }
}