package com.r3.bn.common.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

/**
 * An abstract definition for a flow which allows a business network operator to change the status of a business
 * network membership record. Currently available statuses can be found in the MembershipStatus enum.
 *
 * Typically this flow will be used to approve or reject requests to join a business network. It can also be used to
 * suspend membership as well. Valid status transactions are:
 *
 * 1. NONE - > REQUESTED
 * 2. REQUESTED -> APPROVED
 * 3. REQUESTED -> REJECTED
 * 4. REJECTED -> APPROVED
 * 5. APPROVED -> SUSPENDED
 * 6. SUSPENDED -> APPROVED
 *
 * Assumptions:
 *
 * 1. A business network operator will only ever maintain one record for each identity in a compatibility zone.
 * 2. Currently it is acceptable for the status update process to be started manually by the business network node
 *    operator, probably via some sort of user interface.
 * 3. Only identities with an approved status can participate in the business network.
 * 4. Updates are only signed by the business network operator. The member gets a copy though.
 */
@InitiatingFlow
abstract class AbstractUpdateMembershipRecordStatus : FlowLogic<SignedTransaction>()