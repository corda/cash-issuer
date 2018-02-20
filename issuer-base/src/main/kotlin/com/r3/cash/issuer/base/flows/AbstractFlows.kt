package com.r3.cash.issuer.base.flows

import com.r3.cash.issuer.base.types.RequestToJoinBusinessNetworkResponse
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatingFlow

/**
 * For requests to join the cash business network.
 */
@InitiatingFlow
abstract class JoinBusinessNetworkRequest : FlowLogic<RequestToJoinBusinessNetworkResponse>()

abstract class JoinBusinessNetworkRequestHandler(val otherSession: FlowSession) : FlowLogic<Unit>()

/**
 * For updating the status of a business network member.
 */
@InitiatingFlow
abstract class UpdateBusinessNetworkMembershipStatus : FlowLogic<Unit>()

abstract class UpdateBusinessNetworkMembershipStatusHandler(val otherSession: FlowSession) : FlowLogic<Unit>()

/**
 * For updating bank account information. Members cannot be issued cash until they have registered a bank account with
 * the issuer.
 */
@InitiatingFlow
abstract class UpdateAccountInfoRequest : FlowLogic<Unit>()

abstract class UpdateAccountInfoRequestHandler(val otherSession: FlowSession) : FlowLogic<Unit>()