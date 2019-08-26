package com.allianz.t2i.client.rpc.server.common

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import net.corda.client.rpc.RPCException

/**
 * Represents user defined exception [RPCConnectException] for handling exceptions while connecting to the node
 */
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Cannot connect to the Corda Node")
class RPCConnectException: RuntimeException() {}


/**
 * Represents the user defined exception [RPCFetchException] for handling errors while processing flows, running query etc...
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Cannot fetch")
class RPCFetchException: RuntimeException() {}


