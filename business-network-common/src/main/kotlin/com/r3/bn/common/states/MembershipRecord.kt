package com.r3.bn.common.states

import com.r3.bn.common.commands.Create
import com.r3.bn.common.commands.Update
import com.r3.bn.common.contracts.MembershipRecordContract
import com.r3.bn.common.schemas.MembershipRecordSchemaV1
import com.r3.bn.common.types.MembershipStatus
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.TransactionBuilder
import java.time.Instant

data class MembershipRecord(
        val operator: Party,
        val member: Party,
        val status: MembershipStatus = MembershipStatus.REQUESTED,
        val lastUpdated: Instant = Instant.now(),
        override val participants: List<AbstractParty> = listOf(operator, member),
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    companion object {
        fun createNewRecord(bno: Party, requester: Party, services: ServiceHub): TransactionBuilder {
            // TODO: We need to revisit notary selection for production networks.
            val notary = services.networkMapCache.notaryIdentities.first()
            val command = Command(Create(), listOf(bno.owningKey, requester.owningKey))
            val newRequest = MembershipRecord(bno, requester)
            val stateAndContract = StateAndContract(newRequest, MembershipRecordContract.CONTRACT_ID)
            return TransactionBuilder(notary = notary).withItems(command, stateAndContract)
        }

        fun updateRecordStatus(
                currentRecord: StateAndRef<MembershipRecord>,
                newStatus: MembershipStatus
        ): TransactionBuilder {
            val notary = currentRecord.state.notary
            val inputState = currentRecord.state.data
            val command = Command(Update(), listOf(inputState.operator.owningKey))
            val outputState = inputState.updateStatus(newStatus)
            val stateAndContract = StateAndContract(outputState, MembershipRecordContract.CONTRACT_ID)
            return TransactionBuilder(notary = notary).withItems(command, stateAndContract, currentRecord)
        }
    }

    fun updateStatus(newStatus: MembershipStatus) = copy(status = newStatus)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is MembershipRecordSchemaV1 -> MembershipRecordSchemaV1.PersistentMembershipRecord(
                    this.operator.name.toString(),
                    this.member.name.toString(),
                    this.status,
                    this.lastUpdated,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(MembershipRecordSchemaV1)
}