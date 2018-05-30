package com.r3.bn.common.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.transactions.SignedTransaction

/**
 * An abstract flow definition for requesting to join any business network. This flow definition is shared by both,
 * business network operator and potential business network member, so business network operators can use this abstract
 * flow definition in their [InitiatedBy] flow annotations.
 *
 * All Corda nodes come with the option to install the "business network common" CorDapp so that they can join any type
 * of business network.
 *
 * Some assumptions are made here:
 * 1. There can only be one business network operator per node.
 * 2. Prior to using this flow, Party nodes perform due diligence over the business network operator. Therefore, it will
 *    never be the case where a Party node joins the wrong (or perhaps malicious) business network.
 * 3. For now, it is assumed that the Party node requesting to join the business network already has that business
 *    network operator's CorDapp. This might not be the case going forward though. Perhaps the business network
 *    operator will use the flow framework to transfer the CorDapp to the Party node requesting to join the business
 *    network.
 *
 * The flow requires representing the business network operator of the business network that the caller
 * of this flow wishes to join.
 *
 * The flow returns a [SignedTransaction] containing the business network membership state with a "requested" status.
 */
@InitiatingFlow
abstract class AbstractJoinBusinessNetworkRequest : FlowLogic<SignedTransaction>()