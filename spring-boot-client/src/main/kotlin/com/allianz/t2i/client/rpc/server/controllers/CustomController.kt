package com.allianz.t2i.client.rpc.server.controllers

import net.corda.client.jackson.JacksonSupport
import com.allianz.t2i.client.rpc.server.common.RPCFetchException
import com.allianz.t2i.client.rpc.server.service.StandardService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*



/**
 * Define CorDapp-specific endpoints in a controller such as this.
 */
@RestController
@RequestMapping("/t2i") // The paths for GET and POST requests are relative to this base path.
class CustomController(val standardService: StandardService) {

    /**
     * SLF4J logging
     */
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }


    /**
     * Rest endpoint to add a bank account with account details and node info to which connections is established
     * @param node Node identity to be used for rpc connection
     * @param account ID,Name,Number,sortcode represents account details
     */
    @PostMapping("/addbankaccount")
    private fun addBankAccount(@RequestParam(value="node", defaultValue = "PartyA") node: String,
                                @RequestParam(value="accountId", defaultValue = "12345") accountId: String,
                               @RequestParam(value="accountName", defaultValue = "AGCS SE") accountName: String,
                               @RequestParam(value="accountNumber", defaultValue = "13371337") accountNumber: String,
                               @RequestParam(value="sortCode", defaultValue = "442200") sortCode: String): ResponseEntity<String> {

        try {
            // call to the service for adding bank account
            val rpcResponse = this.standardService.addBankAccount(node,accountId, accountName, accountNumber, sortCode)
            logger.info(rpcResponse.toString())


            if(rpcResponse.status.equals("success")){


                // generate json response
                val mapper = JacksonSupport.createNonRpcMapper()
                val AddBankResponse = mapper.writeValueAsString(rpcResponse)  // myCordaState can be any object.

                return ResponseEntity.ok().body(AddBankResponse)
            }
            else throw RPCFetchException()

        } catch (exception: Exception) {
            throw RPCFetchException()
        }


    }

    /**
     * Rest endpoint for token transfer and node info to which connections is established
     * @param node Node identity to be used for rpc connection
     * @param recipient recipient party identifier
     * @param amount amount to transfer
     */
    @PostMapping( "/tokentransfer")
    private fun tokenTransfer(@RequestParam(value="node", defaultValue = "PartyA") node: String,
                              @RequestParam(value="recipient", defaultValue = "PartyB") recipient: String,
                              @RequestParam(value="amount", defaultValue = "100") amount: String)
                : ResponseEntity<String> {

        try {
            val rpcResponse = this.standardService.tokenTransfer(node, recipient, amount)
            logger.info(rpcResponse.toString())
            if(rpcResponse.status.equals("success")){


                // generate json response
                val mapper = JacksonSupport.createNonRpcMapper()
                val tokenTransferResponse = mapper.writeValueAsString(rpcResponse)  // myCordaState can be any object.

                return ResponseEntity.ok().body(tokenTransferResponse)
            }
            else throw RPCFetchException()

        } catch (exception: Exception) {
            throw RPCFetchException()
        }


    }



    /**
     * Rest endpoint for fetch token balance from vault using vault query
     * @param node Node identity to be used for rpc connection
     */

    @GetMapping( "/tokenbalance")
    private fun getTokenBalance(@RequestParam(value="node", defaultValue = "PartyA") node: String): ResponseEntity<String> {

        try {
            val rpcResponse = this.standardService.getTokenBalance(node)
            logger.info(rpcResponse.toString())
            if(rpcResponse.status.equals("success")){


                // generate json response
                val mapper = JacksonSupport.createNonRpcMapper()
                val addBankResponse = mapper.writeValueAsString(rpcResponse)  // myCordaState can be any object.

                return ResponseEntity.ok().body(addBankResponse)
            }
            else throw RPCFetchException()

        } catch (exception: Exception) {
            throw RPCFetchException()
        }


    }

    /**
     * Rest endpoint for token transfer and node info to which connections is established
     * @param node Node identity to be used for rpc connection
     * @param amount amount to redeem
     */
    @PostMapping( "/tokenredeem")
    private fun tokenTransfer(@RequestParam(value="node", defaultValue = "PartyA") node: String,
                              @RequestParam(value="amount", defaultValue = "100") amount: String)
            : ResponseEntity<String> {

        try {
            val rpcResponse = this.standardService.tokenRedemption(node, amount)
            logger.info(rpcResponse.toString())
            if(rpcResponse.status.equals("success")){


                // generate json response
                val mapper = JacksonSupport.createNonRpcMapper()
                val tokenRedemptionResponse = mapper.writeValueAsString(rpcResponse)  // myCordaState can be any object.

                return ResponseEntity.ok().body(tokenRedemptionResponse)
            }
            else throw RPCFetchException()

        } catch (exception: Exception) {
            throw RPCFetchException()
        }


    }


}