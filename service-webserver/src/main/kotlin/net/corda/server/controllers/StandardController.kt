package net.corda.server.controllers

import net.corda.core.contracts.ContractState
import net.corda.core.messaging.vaultQueryBy
import net.corda.server.NodeRPCConnection
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * A CorDapp-agnostic controller that exposes standard endpoints.
 */
@RestController
@RequestMapping("/") // The paths for GET and POST requests are relative to this base path.
class StandardController(
        private val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping("/status", produces = ["text/plain"])
    private fun status() = "200"

    @GetMapping("/servertime", produces = ["text/plain"])
    private fun serverTime() = LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC")).toString()

    @GetMapping("/addresses", produces = ["text/plain"])
    private fun addresses() = proxy.nodeInfo().addresses.toString()

    @GetMapping("/identities", produces = ["text/plain"])
    private fun identities() = proxy.nodeInfo().legalIdentities.toString()

    @GetMapping("/platformversion", produces = ["text/plain"])
    private fun platformVersion() = proxy.nodeInfo().platformVersion.toString()

    @GetMapping("/peers", produces = ["text/plain"])
    private fun peers() = proxy.networkMapSnapshot().flatMap { it.legalIdentities }.toString()

    @GetMapping("/notaries", produces = ["text/plain"])
    private fun notaries() = proxy.notaryIdentities().toString()

    @GetMapping("/flows", produces = ["text/plain"])
    private fun flows() = proxy.registeredFlows().toString()

    @GetMapping("/states", produces = ["text/plain"])
    private fun states() = proxy.vaultQueryBy<ContractState>().states.toString()
}