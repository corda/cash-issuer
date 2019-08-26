package com.allianz.t2i.client.rpc.server.model

import com.allianz.t2i.common.contracts.states.BankAccountState
import net.corda.core.node.NodeInfo


/**
 * Represents basic response from the server
 */
abstract class BasicResponse() {
    abstract val status: String
}

/**
 * Represents RPCConnect model
 */
data class RPCConnect (override val status: String, val response: String): BasicResponse()


/**
 * Represents response model for Node Info API fetch
 */
data class NodeInfoResponse(override val status: String, val nodeInfo: NodeInfo): BasicResponse()


/**
 * Represents response model for Account Addition
 */
data class AccountAdditionResponse(override val status: String, val response: BankAccountState): BasicResponse()

/**
 * Represents response model for Token Balance fetch
 */
data class TokenBalanceResponse(override val status: String, val tokenBalance: String): BasicResponse()

/**
 * Represents response model for Token Transfer call
 */
data class TokenTransferResponse(override val status: String, val updatedTokenBalance: String): BasicResponse()


/**
 * Represents response model for Token Redemption call
 */
data class TokenRedemptionResponse(override val status: String, val updatedTokenBalance: String): BasicResponse()
