package com.allianz.t2i.client.rpc.server.service

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.money.EUR
import com.allianz.t2i.common.contracts.schemas.BankAccountStateSchemaV1
import com.allianz.t2i.common.contracts.states.BankAccountState
import com.allianz.t2i.common.contracts.types.AccountNumber
import com.allianz.t2i.common.contracts.types.BankAccount
import com.allianz.t2i.common.contracts.types.UKAccountNumber
import com.allianz.t2i.common.workflows.flows.AddBankAccount
import com.allianz.t2i.common.workflows.flows.MoveCashShell
import net.corda.client.rpc.internal.ReconnectingCordaRPCOps
import net.corda.core.flows.StateMachineRunId
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
import net.corda.core.utilities.NetworkHostAndPort
import com.allianz.t2i.client.rpc.server.common.RPCConnectException
import com.allianz.t2i.client.rpc.server.common.RPCFetchException
import com.allianz.t2i.client.rpc.server.model.*
import com.allianz.t2i.client.workflows.flows.RedeemCashShell
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment

import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController


private const val CORDA_USER_NAME = "config.rpc.username"
private const val CORDA_USER_PASSWORD = "config.rpc.password"

private const val NODE_A_IDENTIFIER = "nodea.rpc.identifier"
private const val NODE_A_HOST = "nodea.rpc.host"
private const val NODE_A_PORT = "nodea.rpc.port"

private const val NODE_B_IDENTIFIER = "nodeb.rpc.identifier"
private const val NODE_B_HOST = "nodeb.rpc.host"
private const val NODE_B_PORT = "nodeb.rpc.port"


/**
 * Interface to define the standard services that can be used
 */

interface StandardService {

    val env: Environment


    /**
     * Returns ReconnectingCordaRPCOps which is a wrapper over [CordaRPCOps]
     */

    fun connectRPC(node: String,
                   username: String = env.getProperty(CORDA_USER_NAME)!!,
                   password: String = env.getProperty(CORDA_USER_PASSWORD)!!
    ): ReconnectingCordaRPCOps


    /**
     * Returns node information for the given node identity by the vault query
     */

    fun getNodeInfo(node: String): NodeInfoResponse


    /**
     * Adds the bank account
     */

    fun addBankAccount(node: String,accountId: String, accountName: String, accountNumber: String, sortCode: String): AccountAdditionResponse


    /**
     * Returns aggregated token balance for the given node/ identity represented by the node
     */

    fun getTokenBalance(node: String): TokenBalanceResponse


    /**
     * Initiates the token transfer
     *
     */

    fun tokenTransfer(node: String,
                      recipient: String,
                      amount: String): TokenTransferResponse


    /**
     * Initiates the process for redemption of tokens requested
     */
    fun tokenRedemption(node: String,
                        amount: String) : TokenRedemptionResponse
}

/**
 * Standard Service Implementation
 */
@Service
open class StandardServiceImpl(@Autowired override val env: Environment) : StandardService {



    /**
     * SLF4J logging
     */
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }





    /**
     * Returns ReconnectingCordaRPCOps which is a wrapper over [CordaRPCOps]
     * @param node identity of the node to connect
     * @param username username for connect
     * @param password password for connect
     */

    override fun connectRPC(node: String,
                            username: String,
                            password: String): ReconnectingCordaRPCOps {
        try {

            var host: String? = null

            var rpcPort: Int? = null

            logger.info("connectRPC method call ")

            when(node) {
                this.env.getProperty(NODE_A_IDENTIFIER) -> {
                    host = env.getProperty(NODE_A_HOST)!!
                    rpcPort =  env.getProperty(NODE_A_PORT)!!.toInt()
            }
                this.env.getProperty(NODE_B_IDENTIFIER) -> {
                    host = env.getProperty(NODE_B_HOST)!!
                    rpcPort =  env.getProperty(NODE_B_PORT)!!.toInt()
                }
            }

            val rpcAddress = NetworkHostAndPort(host!!, rpcPort!!)
            val rpcConnection = ReconnectingCordaRPCOps(rpcAddress, username, password)
            if (rpcConnection.nodeInfo().legalIdentities.isNotEmpty()) {
                return rpcConnection
            } else {
                throw RPCConnectException()
            }
        } catch (exception: Exception) {
                throw RPCConnectException()
        }
    }



    /**
     * Returns node information for the given node identity by rpc function [CordaRPCOps.nodeInfo]
     */

    override fun getNodeInfo(node: String): NodeInfoResponse {



        try {

            /** establish RPC Connection using the [connectRPC] method
             * This uses the [use] inline function to execute the given block function on this resource
             * and then closes it down correctly whether an exception is thrown or not.
             */
            connectRPC(node).use { rpcConnection ->

                return NodeInfoResponse(status = "success", nodeInfo = rpcConnection.nodeInfo())

            }



        } catch (exception: Exception) {
            throw RPCFetchException()
        }


    }

    /**
     * Adds the bank account by initiating the flow [AddBankAccount]
     * * It first creates the rpc connection and then uses it to run the flow using [runFlowWithLogicalRetry]
     * This provides us the Observables that can be subscribed
     * Confirms the success by a query to check if the created bank account state is present in the vault
     */
    override fun addBankAccount(node: String,
                                accountId: String,
                                accountName: String,
                                accountNumber: String,
                                sortCode: String)
            : AccountAdditionResponse {

        try {


            /** establish RPC Connection using the [connectRPC] method
             * This uses the [use] inline function to execute the given block function on this resource
             * and then closes it down correctly whether an exception is thrown or not.
             */
            connectRPC(node).use { rpcConnection ->


                // Start nrOfFlowsToRun and provide a logical retry function that checks the vault.
                val flowProgressEvents = mutableMapOf<StateMachineRunId, MutableList<String>>()

                // flow start AddBankAccount bankAccount: { accountId: 12345, accountName: Rogers Account, accountNumber: { sortCode: 442200, accountNumber: 13371337, type: uk }, currency: { tokenIdentifier: EUR, fractionDigits: 2} }, verifier: Issuer


                val accountnumber: AccountNumber = UKAccountNumber(sortCode = sortCode, accountNumber = accountNumber)
                val bankAccount = BankAccount(accountId = accountId, accountName = accountName, accountNumber = accountnumber, currency = EUR)

                val issuerRef = rpcConnection.partiesFromName(query = "Issuer", exactMatch = true)


                // DOCSTART rpcReconnectingRPCFlowStarting
                rpcConnection.runFlowWithLogicalRetry(
                        runFlow = { rpc ->
                            logger.debug("Starting addBankAccount for $accountName")
                            val flowHandle = rpc.startTrackedFlowDynamic(
                                    AddBankAccount::class.java,
                                    bankAccount,
                                    issuerRef.first()
                            )
                            val flowId = flowHandle.id
                            logger.info("Add bank account flow started flow $accountName with flowId: $flowId")
                            flowProgressEvents.addEvent(flowId, null)

                            // No reconnecting possible.
                            flowHandle.progress.subscribe(
                                    { prog ->
                                        flowProgressEvents.addEvent(flowId, prog)
                                        logger.info("Progress $flowId : $prog")
                                    },
                                    { error ->
                                        logger.error("Error thrown in the flow progress observer", error)
                                    })
                            flowHandle.id
                        },
                        hasFlowStarted = { rpc ->

                            // Query to check if the created bank account state is present in the vault
                            val criteria = QueryCriteria.VaultCustomQueryCriteria(builder { BankAccountStateSchemaV1.PersistentBankAccountState::accountName.equal(accountName) }, status = Vault.StateStatus.ALL)
                            val results = rpc.vaultQueryByCriteria(criteria, BankAccountState::class.java)

                            logger.debug("Bank Account State found - Found states ${results.states}")

                            // The flow has completed if a state is found
                            results.states.isNotEmpty()

                        },
                        onFlowConfirmed = {

                            logger.debug("Flow started for AddBankAccount.")
                        }
                )


                // Wait for all events to come in and flows to finish.
                Thread.sleep(6000)


                logger.debug("outside runFlowWithLogicalRetry")

                // Query to check if the created bank account state is present in the vault
                val criteria = QueryCriteria.VaultCustomQueryCriteria(builder { BankAccountStateSchemaV1.PersistentBankAccountState::accountName.equal(accountName) }, status = Vault.StateStatus.ALL)
                val results = rpcConnection.vaultQueryByCriteria(criteria, BankAccountState::class.java)
                val addedBankAccountState: BankAccountState = results.states.first().state.data

                logger.info("Bank Account State found added- Found states ${results.states}")
                return AccountAdditionResponse(status = "success", response = addedBankAccountState)

                // DOCEND rpcReconnectingRPCFlowStarting
            }

        } catch (exception: Exception) {
            throw RPCFetchException()
        }

    }


    /**
     * Returns aggregated token balance for the given node/ identity represented by the node
     *
     * First fetches the total unconsumed states of type Fungible Token and then aggregates the value for amount field using Map and Reduce
     */

    override fun getTokenBalance(node: String): TokenBalanceResponse {

        try {

            /** establish RPC Connection using the [connectRPC] method
             * This uses the [use] inline function to execute the given block function on this resource
             * and then closes it down correctly whether an exception is thrown or not.
             */
            connectRPC(node).use { rpcConnection ->

                // Fetches the unconsumed fungible token states
                val unconsumedTokenStates = rpcConnection.vaultQueryByCriteria(contractStateType = FungibleToken::class.java, criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED))
                        .states

                logger.debug("total count of states: ${unconsumedTokenStates.count()}")

                // aggregate using map reduce
                val totalTokenValue = (unconsumedTokenStates.map { it.state.data.amount.quantity }.toList().reduce { sum, l -> sum + l }) / 100

                logger.info("Token Value after transfer: $totalTokenValue")

                return TokenBalanceResponse(status = "success", tokenBalance = totalTokenValue.toString())
            }
        }catch (exception: Exception) {
            throw RPCFetchException()
        }

    }

    /**
     * Initiates the token transfer using the flow [MoveCashShell]
     * It first creates the rpc connection and then uses it to run the flow using [runFlowWithLogicalRetry]
     * This provides us the Observables that can be subscribed
     * Confirms the success by checking if the total token balance is reduced after the transfer

     */
    override fun tokenTransfer(node: String,
                               recipient: String,
                               amount: String)
            : TokenTransferResponse {

        try {

            /** establish RPC Connection using the [connectRPC] method
             * This uses the [use] inline function to execute the given block function on this resource
             * and then closes it down correctly whether an exception is thrown or not.
             */
            connectRPC(node).use { rpcConnection ->


                // Recording of flow progress in a map
                val flowProgressEvents = mutableMapOf<StateMachineRunId, MutableList<String>>()

                // the flow that is invoked -   flow start MoveCashShell recipient: "O=PartyB, L=New York, C=US", amount: 7930, currency: "EUR"
                val recipientParty = rpcConnection.partiesFromName(query = recipient, exactMatch = true).first()

                logger.debug("Receiving Party: $recipientParty")

                /**
                 * Temporary solution to find out if the token transfer flow completed successfully by recording the token before transfer
                 * and comparing it with the token balance after transfer
                 * This logic cannot be trusted if we have multiple flows happening at the same time which can alter the token balance
                 * Aggregation token balance using map and reduce
                 */
                val unconsumedTokenStatesBefore = rpcConnection.vaultQueryByCriteria(contractStateType = FungibleToken::class.java, criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED))
                        .states


                val totalTokenValueBefore = (unconsumedTokenStatesBefore.map { it.state.data.amount.quantity }.toList().reduce { sum, l -> sum + l }) / 100

                logger.info("Token Value before transfer: $totalTokenValueBefore")

                // DOCSTART rpcReconnectingRPCFlowStarting
                rpcConnection.runFlowWithLogicalRetry(
                        runFlow = { rpc ->
                            logger.info("Starting Token Transfer of $amount to $recipient ")
                            val flowHandle = rpc.startTrackedFlowDynamic(
                                    MoveCashShell::class.java,
                                    recipientParty,
                                    amount.toLong(),
                                    "EUR"
                            )
                            val flowId = flowHandle.id
                            logger.info("Token transfer flow started:  flow MoveCashShell with flowId: $flowId")
                            flowProgressEvents.addEvent(flowId, null)

                            // No reconnecting possible.
                            flowHandle.progress.subscribe(
                                    { prog ->
                                        flowProgressEvents.addEvent(flowId, prog)
                                        logger.info("Progress $flowId : $prog")
                                    },
                                    { error ->
                                        logger.error("Error thrown in the flow progress observer", error)
                                    })
                            flowHandle.id
                        },
                        hasFlowStarted = { rpc ->

                            logger.debug("hasFlowStarted")

                            val unconsumedTokenStatesAfter = rpc.vaultQueryByCriteria(contractStateType = FungibleToken::class.java, criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED))
                                    .states

                            val totalTokenValueAfter = (unconsumedTokenStatesAfter.map { it.state.data.amount.quantity }.toList().reduce { sum, l -> sum + l }) / 100

                            logger.info("Token Value after transfer: $totalTokenValueAfter")


                            totalTokenValueAfter < totalTokenValueBefore
                        },
                        onFlowConfirmed = {

                            logger.info("Flow started for Token Transfer.")
                        }
                )


                // Wait for all events to come in and flows to finish.
                Thread.sleep(6000)


                logger.debug("outside runFlowWithLogicalRetry")


                /**
                 * Second part of the token balance checking step to find out if the balance has been reduced. This confirms the flow completion
                 *
                 */
                val unconsumedTokenStatesAfter = rpcConnection.vaultQueryByCriteria(contractStateType = FungibleToken::class.java, criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED))
                        .states

                val totalTokenValueAfter = (unconsumedTokenStatesAfter.map { it.state.data.amount.quantity }.toList().reduce { sum, l -> sum + l }) / 100

                logger.info("Token Value after transfer: $totalTokenValueAfter")

                return TokenTransferResponse(status = "success", updatedTokenBalance = totalTokenValueAfter.toString())


                // DOCEND rpcReconnectingRPCFlowStarting
            }
        } catch (exception: Exception) {
            throw RPCFetchException()
        }


    }


    /**
     * Initiates the process of token redemption by initiating the flow [RedeemCashShell]
     */
    override fun tokenRedemption(node: String, amount: String): TokenRedemptionResponse {

            try{
                /** establish RPC Connection using the [connectRPC] method
                 * This uses the [use] inline function to execute the given block function on this resource
                 * and then closes it down correctly whether an exception is thrown or not.
                 */
                connectRPC(node).use { rpcConnection ->

                    // Recording of flow progress in a map
                    val flowProgressEvents = mutableMapOf<StateMachineRunId, MutableList<String>>()


                    // the flow that is invoked -   flow start RedeemCashShell  amount: 500, currency: "EUR", issuer: Issuer

                    val issuerRef = rpcConnection.partiesFromName(query = "Issuer", exactMatch = true)



                    /**
                     * Temporary solution to find out if the token redemption flow completed successfully by recording the token before redemption
                     * and comparing it with the token balance after redemption
                     * This logic cannot be trusted if we have multiple flows happening at the same time which can alter the token balance
                     * Aggregation of token balance using map and reduce
                     */
                    val unconsumedTokenStatesBefore = rpcConnection.vaultQueryByCriteria(contractStateType = FungibleToken::class.java, criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED))
                            .states


                    val totalTokenValueBefore = (unconsumedTokenStatesBefore.map { it.state.data.amount.quantity }.toList().reduce { sum, l -> sum + l }) / 100

                    logger.info("Token Value before redemption: $totalTokenValueBefore")




                    // DOCSTART rpcReconnectingRPCFlowStarting
                    rpcConnection.runFlowWithLogicalRetry(
                            runFlow = { rpc ->
                                logger.debug("Starting Redemption Flow for $node")
                                val flowHandle = rpc.startTrackedFlowDynamic(
                                        RedeemCashShell::class.java,
                                        amount.toLong(),
                                        "EUR",
                                        issuerRef.first()
                                )
                                val flowId = flowHandle.id
                                logger.info("Redeem Cash flow started: Redeem $amount with flowId: $flowId")
                                flowProgressEvents.addEvent(flowId, null)

                                // No reconnecting possible.
                                flowHandle.progress.subscribe(
                                        { prog ->
                                            flowProgressEvents.addEvent(flowId, prog)
                                            logger.info("Progress $flowId : $prog")
                                        },
                                        { error ->
                                            logger.error("Error thrown in the flow progress observer", error)
                                        })
                                flowHandle.id
                            },
                            hasFlowStarted = { rpc ->


                                val unconsumedTokenStatesAfter = rpc.vaultQueryByCriteria(contractStateType = FungibleToken::class.java, criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED))
                                        .states

                                val totalTokenValueAfter = (unconsumedTokenStatesAfter.map { it.state.data.amount.quantity }.toList().reduce { sum, l -> sum + l }) / 100

                                logger.info("Token Value after redemption: $totalTokenValueAfter")


                                totalTokenValueAfter < totalTokenValueBefore


                            },
                            onFlowConfirmed = {

                                logger.debug("Flow started for AddBankAccount.")
                            }
                    )

                    // Wait for all events to come in and flows to finish.
                    Thread.sleep(6000)


                    logger.debug("outside runFlowWithLogicalRetry")


                    /**
                     * Second part of the token balance checking step to find out if the balance has been reduced. This confirms the flow completion
                     *
                     */
                    val unconsumedTokenStatesAfter = rpcConnection.vaultQueryByCriteria(contractStateType = FungibleToken::class.java, criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED))
                            .states

                    val totalTokenValueAfter = (unconsumedTokenStatesAfter.map { it.state.data.amount.quantity }.toList().reduce { sum, l -> sum + l }) / 100

                    logger.info("Token Value after transfer: $totalTokenValueAfter")

                    return TokenRedemptionResponse(status = "success", updatedTokenBalance = totalTokenValueAfter.toString())


                // DOCEND rpcReconnectingRPCFlowStarting



                }

            } catch (exception: Exception) {
                throw RPCFetchException()
            }

    }




    @Synchronized
    fun MutableMap<StateMachineRunId, MutableList<String>>.addEvent(id: StateMachineRunId, progress: String?): Boolean {
        return getOrPut(id) { mutableListOf() }.let { if (progress != null) it.add(progress) else false }
    }

}