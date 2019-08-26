package com.allianz.t2i.client.rpc.server.controllers

import net.corda.client.jackson.JacksonSupport
import com.allianz.t2i.client.rpc.server.model.RPCConnect
import com.allianz.t2i.client.rpc.server.common.RPCConnectException
import com.allianz.t2i.client.rpc.server.common.RPCFetchException
import com.allianz.t2i.client.rpc.server.service.StandardService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * A CorDapp-agnostic controller that exposes standard endpoints.
 */
@RestController
@RequestMapping("/") // The paths for GET and POST requests are relative to this base path.
class StandardController(val standardService: StandardService) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }


    /**
     * Rest endpoint to check status of spring server
     */
    @GetMapping("/status", produces = arrayOf("text/plain"))
    private fun status() = "200"


    /**
     * Rest endpoint for fetch node information
     */
    @GetMapping("/nodeinfo")
    private fun getNodeInfo(@RequestParam(value="node", defaultValue = "PartyA") node: String): ResponseEntity<String> {

        try {
            val rpcResponse = this.standardService.getNodeInfo(node)

            // generate json response
            val mapper = JacksonSupport.createNonRpcMapper()
            val nodeInfoJson = mapper.writeValueAsString(rpcResponse)  // myCordaState can be any object.

            logger.info(rpcResponse.toString())
            if(rpcResponse.status.equals("success")){
                return ResponseEntity.ok().body(nodeInfoJson)
            }
            else throw RPCFetchException()

        } catch (exception: Exception) {
            throw RPCFetchException()
        }


    }
//
//
//
//    @GetMapping(value = "/servertime", produces = arrayOf("text/plain"))
//    private fun serverTime() = LocalDateTime.ofInstant(standardService.rpcConnection.proxy.currentNodeTime(), ZoneId.of("UTC")).toString()
//
//    @GetMapping(value = "/addresses", produces = arrayOf("text/plain"))
//    private fun addresses() = standardService.rpcConnection.proxy.nodeInfo().addresses.toString()
//
//    @GetMapping(value = "/identities", produces = arrayOf("text/plain"))
//    private fun identities() = standardService.rpcConnection.proxy.nodeInfo().legalIdentities.toString()
//
//    @GetMapping(value = "/platformversion", produces = arrayOf("text/plain"))
//    private fun platformVersion() = standardService.rpcConnection.proxy.nodeInfo().platformVersion.toString()
//
//    @GetMapping(value = "/peers", produces = arrayOf("text/plain"))
//    private fun peers() = standardService.rpcConnection.proxy.networkMapSnapshot().flatMap { it.legalIdentities }.toString()
//
//    @GetMapping(value = "/notaries", produces = arrayOf("text/plain"))
//    private fun notaries() = standardService.rpcConnection.proxy.notaryIdentities().toString()
//
//    @GetMapping(value = "/flows", produces = arrayOf("text/plain"))
//    private fun flows() = standardService.rpcConnection.proxy.registeredFlows().toString()
//
//    @GetMapping(value = "/states", produces = arrayOf("text/plain"))
//    private fun states() = standardService.rpcConnection.proxy.vaultQueryBy<ContractState>().states.toString()
}