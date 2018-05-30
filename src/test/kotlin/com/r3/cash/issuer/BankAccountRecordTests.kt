package com.r3.cash.issuer

import com.r3.bn.common.types.MembershipStatus
import com.r3.bn.operator.flows.UpdateMembershipRecordStatus
import com.r3.bn.user.flows.JoinBusinessNetworkRequest
import com.r3.cash.issuer.client.flows.CreateBankAccountRecord
import com.r3.testutils.MockNetworkTest
import net.corda.core.flows.FlowException
import net.corda.core.utilities.getOrThrow
import net.corda.node.internal.StartedNode
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.startFlow
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class BankAccountRecordTests : MockNetworkTest(numberOfNodes = 1) {

    lateinit var I: StartedNode<MockNetwork.MockNode>
    lateinit var A: StartedNode<MockNetwork.MockNode>

    @Before
    override fun initialiseNodes() {
        I = nodes[0]
        A = nodes[1]
    }

    @Test
    fun `join then get approved then add a bank account`() {
        val issuer = I.info.legalIdentities.first()
        val party = A.info.legalIdentities.first()
        A.services.startFlow(JoinBusinessNetworkRequest(issuer))
        network.waitQuiescent()
        val updateMembershipRecordResult = I.services.startFlow(UpdateMembershipRecordStatus(party, MembershipStatus.APPROVED))
        network.waitQuiescent()
        val txI = updateMembershipRecordResult.resultFuture.get()
        val txA = A.services.validatedTransactions.getTransaction(txI.id)
        assertEquals(txI, txA)
        val createBankAccountRecordFlow = CreateBankAccountRecord(
                issuer = issuer,
                accountName = "A Ltd",
                accountNumber = "12345678",
                sortCode = "123456"
        )
        val createBankRecordResult = A.services.startFlow(createBankAccountRecordFlow)
        network.waitQuiescent()
        val newBankAccountA = createBankRecordResult.resultFuture.get()
        val newBankAccountI = I.services.validatedTransactions.getTransaction(newBankAccountA.id)
        assertEquals(newBankAccountA, newBankAccountI)
    }

    @Test(expected = FlowException::class)
    fun `join then try to add an account without approval`() {
        val issuer = I.info.legalIdentities.first()
        A.services.startFlow(JoinBusinessNetworkRequest(issuer))
        network.waitQuiescent()
        val createBankAccountRecordFlow = CreateBankAccountRecord(
                issuer = issuer,
                accountName = "A Ltd",
                accountNumber = "12345678",
                sortCode = "123456"
        )
        A.services.startFlow(createBankAccountRecordFlow).resultFuture.getOrThrow()
        network.waitQuiescent()
    }
}