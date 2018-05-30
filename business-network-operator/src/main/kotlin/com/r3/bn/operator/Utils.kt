package com.r3.bn.operator

import com.r3.bn.common.schemas.MembershipRecordSchemaV1
import com.r3.bn.common.states.MembershipRecord
import com.r3.bn.common.types.MembershipStatus
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder

/**
 * Returns the [MembershipRecord] for an identity or null if the record does not exist.
 */
fun getMemberByX500Name(maybeMember: CordaX500Name, services: ServiceHub): StateAndRef<MembershipRecord>? {
    val builder = builder {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val memberName = MembershipRecordSchemaV1.PersistentMembershipRecord::member.equal(maybeMember.toString())
        val customCriteria = QueryCriteria.VaultCustomQueryCriteria(memberName)
        generalCriteria.and(customCriteria)
    }

    val result = services.vaultService.queryBy<MembershipRecord>(builder)
    return result.states.singleOrNull()
}

/**
 * Returns true if the supplied identity has a [MembershipRecord] with the calling business network operator.
 */
fun hasMembershipRecord(maybeMember: CordaX500Name, services: ServiceHub): Boolean {
    return getMemberByX500Name(maybeMember, services) != null
}

fun MembershipRecord.isApproved() = this.status == MembershipStatus.APPROVED
fun MembershipRecord.isNotApproved() = this.status != MembershipStatus.APPROVED