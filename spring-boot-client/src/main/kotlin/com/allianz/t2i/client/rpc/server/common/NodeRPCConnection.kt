package com.allianz.t2i.client.rpc.server.common

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCClientConfiguration
import net.corda.client.rpc.CordaRPCConnection
import net.corda.client.rpc.RPCException
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.StateMachineUpdate
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.seconds
import org.apache.activemq.artemis.api.core.ActiveMQSecurityException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController
import rx.Subscription
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.PreDestroy

private const val CORDA_USER_NAME = "config.rpc.username"
private const val CORDA_USER_PASSWORD = "config.rpc.password"
private const val CORDA_NODE_HOST = "config.rpc.host"
private const val CORDA_RPC_PORT = "config.rpc.port"

/**
 * Wraps a node RPC proxy.
 *
 * The RPC proxy is configured based on the properties in `application.properties`.
 *
 * @property proxy The RPC proxy.
 * @property rpcConnection The RPC connection established
 */

@Lazy
@Component
open class NodeRPCConnection(
//        @Value("\${$CORDA_NODE_HOST}") private val host: String,
//        @Value("\${$CORDA_RPC_PORT}") private val rpcPort: Int
//        @Value("\${$CORDA_NODE_HOST}") private val host: String,
//        @Value("\${$CORDA_RPC_PORT}") private val rpcPort: Int
        ): AutoCloseable {

     lateinit var rpcConnection: CordaRPCConnection
        private set
     lateinit var proxy: CordaRPCOps
        private set

    /**
     * SLF4J logging
     */
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }




    /**
     * Sample implementation for establishing RPC connection with the provided details
     * Returns if the connection established or not
     * @return success/failed
     * @param host hostname for the node to connect
     * @param rpcPort port of the node to connect
     * @param username username for connect
     * @param password password for connect
     */
    fun initialiseNodeRPCConnection(host: String,
                                    rpcPort: Int,
                                    username: String,
                                     password: String
                        ): String {

        try {


            logger.info("NodeRPCConnection initiatisation ")
            logger.info("username: $username password:$password ")

            val rpcAddress = NetworkHostAndPort(host, rpcPort)
            performRpcReconnect(listOf(rpcAddress), username, password)

            proxy = rpcConnection.proxy

            return "success"

        } catch (exception: Exception) {

            return "failed"
        }

    }


    /**
     * Returns [CordaRPCConnection] with connection retry mechanism in case of fail overs
     * @param nodeHostAndPorts list of host:port
     * @param username rpc username
     * @param password rpc password
     */
    private fun establishConnectionWithRetry(nodeHostAndPorts: List<NetworkHostAndPort>, username: String, password: String): CordaRPCConnection {
        val retryInterval = 5.seconds
        var connection: CordaRPCConnection?
        do {
            connection = try {
                logger.info("Connecting to: $nodeHostAndPorts")
                val client = CordaRPCClient(
                        nodeHostAndPorts,
                        CordaRPCClientConfiguration(connectionMaxRetryInterval = retryInterval)
                )
                val _connection = client.start(username, password)
                // Check connection is truly operational before returning it.
                val nodeInfo = _connection.proxy.nodeInfo()
                require(nodeInfo.legalIdentitiesAndCerts.isNotEmpty())
                _connection
            } catch (secEx: ActiveMQSecurityException) {
                // Happens when incorrect credentials provided - no point retrying connection
                logger.info("Security exception upon attempt to establish connection: " + secEx.message)
                throw secEx
            } catch (ex: RPCException) {
                logger.info("Exception upon attempt to establish connection: " + ex.message)
                null    // force retry after sleep
            }
            // Could not connect this time round - pause before giving another try.
            Thread.sleep(retryInterval.toMillis())
        } while (connection == null)

        logger.info("Connection successfully established with: ${connection.proxy.nodeInfo()}")
        rpcConnection = connection
        return connection
    }


    /**
     * Returns [CordaRPCConnection] based on the observable used for automatic reconnection of RPC
     * @param nodeHostAndPorts list of host:port
     * @param username rpc username
     * @param password rpc password
     *
     */
    fun performRpcReconnect(nodeHostAndPorts: List<NetworkHostAndPort>, username: String, password: String): CordaRPCConnection {
        rpcConnection = establishConnectionWithRetry(nodeHostAndPorts, username, password)
        logger.info("connection status: " + rpcConnection.toString())
        proxy = rpcConnection.proxy

        val (stateMachineInfos, stateMachineUpdatesRaw) = proxy.stateMachinesFeed()

        val retryableStateMachineUpdatesSubscription: AtomicReference<Subscription?> = AtomicReference(null)
        val subscription: Subscription = stateMachineUpdatesRaw
                .startWith(stateMachineInfos.map { StateMachineUpdate.Added(it) })
                .subscribe({
                    logger.info("subscription called")
                }, {
                    // Terminate subscription such that nothing gets past this point to downstream Observables.
                    retryableStateMachineUpdatesSubscription.get()?.unsubscribe()
                    // It is good idea to close connection to properly mark the end of it. During re-connect we will create a new
                    // client and a new connection, so no going back to this one. Also the server might be down, so we are
                    // force closing the connection to avoid propagation of notification to the server side.
                    rpcConnection.forceClose()
                    // Perform re-connect.
                    logger.info("performRpcReconnect")
                    performRpcReconnect(nodeHostAndPorts, username, password)
                })

        retryableStateMachineUpdatesSubscription.set(subscription)

        return rpcConnection
    }

    @PreDestroy
    override fun close() {
        rpcConnection.notifyServerAndClose()
    }


}