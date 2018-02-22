package com.r3.bn.common.contracts

import com.r3.bn.common.commands.Create
import com.r3.bn.common.commands.Update
import com.r3.bn.common.states.MembershipRecord
import com.r3.bn.common.types.MembershipStatus
import net.corda.core.identity.CordaX500Name
import net.corda.testing.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class MembershipRecordContractTests {
    private val corDapps = listOf("com.r3.bn.common")
    private val ledgerServices = MockServices(corDapps)

    private val issuer = TestIdentity(CordaX500Name("Issuer", "London", "GB"))
    private val partyA = TestIdentity(CordaX500Name("Party A", "London", "GB"))

    @Test
    fun `create new membership record`() {
        val newRecord = MembershipRecord(issuer.party, partyA.party)
        ledgerServices.ledger {
            transaction {
                output(MembershipRecordContract.CONTRACT_ID, newRecord)
                command(listOf(issuer.publicKey, partyA.publicKey), Create())
                verifies()
            }
        }
    }

    @Test
    fun `update membership record`() {
        val baseRecord = MembershipRecord(issuer.party, partyA.party)
        ledgerServices.ledger {
            transaction {
                input(MembershipRecordContract.CONTRACT_ID, baseRecord)
                output(MembershipRecordContract.CONTRACT_ID, baseRecord.updateStatus(MembershipStatus.APPROVED))
                command(listOf(issuer.publicKey), Update())
                verifies()
            }

            transaction {
                input(MembershipRecordContract.CONTRACT_ID, baseRecord)
                output(MembershipRecordContract.CONTRACT_ID, baseRecord.updateStatus(MembershipStatus.REJECTED))
                command(listOf(issuer.publicKey), Update())
                verifies()
            }

            transaction {
                input(MembershipRecordContract.CONTRACT_ID, baseRecord)
                output(MembershipRecordContract.CONTRACT_ID, baseRecord.updateStatus(MembershipStatus.SUSPENDED))
                command(listOf(issuer.publicKey), Update())
                fails()
            }
        }
    }
}