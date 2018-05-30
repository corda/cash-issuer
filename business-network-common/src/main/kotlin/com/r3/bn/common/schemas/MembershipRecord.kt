package com.r3.bn.common.schemas

import com.r3.bn.common.types.MembershipStatus
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.time.Instant
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object MembershipRecordSchema

object MembershipRecordSchemaV1 : MappedSchema(
        schemaFamily = MembershipRecordSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentMembershipRecord::class.java)
) {

    @Entity
    @Table(name = "membership_records")
    class PersistentMembershipRecord(
            @Column(name = "operator")
            var operator: String,
            @Column(name = "member")
            var member: String,
            @Column(name = "status")
            var status: MembershipStatus,
            @Column(name = "last_updated")
            var lastUpdated: Instant,
            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor() : this(
                operator = "",
                member = "",
                status = MembershipStatus.REQUESTED,
                lastUpdated = Instant.now(),
                linearId = UUID.randomUUID()
        )
    }

}