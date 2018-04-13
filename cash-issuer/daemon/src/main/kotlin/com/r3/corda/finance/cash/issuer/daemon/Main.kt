package com.r3.corda.finance.cash.issuer.daemon

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import java.util.*

fun help() {
    println("START -> Start polling")
    println("STOP -> Stop polling")
}

fun prompt() {
    println("Enter a command: ")
    print("> ")
}

fun main(args: Array<String>) {
    require(args.size == 1) { "Please enter a host name port for the node to connect to." }

    val nodeAddress = parse(args[0])
    val client = CordaRPCClient(nodeAddress)
    val proxy = client.start("user1", "test").proxy

    val scanner = Scanner(System.`in`)

    help()
    prompt()

    while (true) {
        val command = scanner.nextLine()
        when (command) {
            "start" -> {

            }
            "stop" -> {

            }
            else -> println("What you say?! Type \"help\" for help.")
        }
    }
}