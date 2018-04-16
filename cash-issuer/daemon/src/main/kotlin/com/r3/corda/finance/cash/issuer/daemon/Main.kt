package com.r3.corda.finance.cash.issuer.daemon

import joptsimple.OptionException
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import java.util.*
import kotlin.system.exitProcess

private fun parseArguments(vararg args: String): Pair<ArgsParser, CommandLineOptions> {
    val argsParser = ArgsParser()
    val cmdlineOptions = try {
        argsParser.parse(*args)
    } catch (ex: OptionException) {
        println("Invalid command line arguments: ${ex.message}")
        argsParser.printHelp(System.out)
        exitProcess(1)
    }
    return Pair(argsParser, cmdlineOptions)
}

// Connects to a Corda node specified by a hostname and port using the provided user name and pawssword.
private fun connectToCordaRpc(hostAndPort: String, username: String, password: String): CordaRPCOps {
    val nodeAddress = parse(hostAndPort)
    val client = CordaRPCClient(nodeAddress)
    return client.start(username, password).proxy
}

fun main(args: Array<String>) {
    val (argsParser, cmdLineOptions) = parseArguments(*args)
    val services = connectToCordaRpc(cmdLineOptions.rpcHostAndPort, cmdLineOptions.rpcUser, cmdLineOptions.rpcPass)
    val scanner = Scanner(System.`in`)
    val daemon = if (cmdLineOptions.mockMode) MockDaemon(services, cmdLineOptions) else Daemon(services, cmdLineOptions)
}