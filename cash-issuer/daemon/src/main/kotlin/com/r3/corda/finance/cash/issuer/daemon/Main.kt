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
    println("Connecting to Issuer node $hostAndPort.")
    val nodeAddress = parse(hostAndPort)
    val client = CordaRPCClient(nodeAddress)
    val cordaRpcOps = client.start(username, password).proxy
    println("Connected!")
    return cordaRpcOps
}

private fun welcome() {
    println()
    println("Welcome to the Corda cash issuer daemon.")
    println()
    println("           .-\"\"-.         ");
    println("          /" + "[O]" + " __\\         ")
    println("         _|__" + "o" + " LI|_     -------- I'm R3D2, the Corda Cash Issuer Daemon! ")
    println("        / | " + "====" + " | \\       ")
    println("        |_| " + "====" + " |_|        ")
    println("         " + "|" + "|\" ||  |" + "|" + "        ")
    println("         " + "|" + "|LI  " + "o" + " |" + "|" + "         ")
    println("         " + "|" + "|'----'|" + "|" + "         ")
    println("        /__|    |__\\       ")
    println()
}

private fun repl(daemon: Daemon, cmdLineOptions: CommandLineOptions) {
    fun prompt() = print("> ")
    val scanner = Scanner(System.`in`)

    if (cmdLineOptions.autoMode) {
        println("\nAuto-mode set to TRUE. ")
        println("Polling for transactions from all registered APIs at FIVE second intervals...")
    } else {
        print("\nEnter a command ")
        prompt()
    }

    while (true) {
        val command = scanner.nextLine()
        when (command) {
            "start" -> {
                println("Polling for transactions from all registered APIs at FIVE second intervals...")
                daemon.start()
            }
            "stop" -> {
                daemon.stop()
                prompt()
            }
            "quit" -> {
                println("Bye bye!")
                exitProcess(0)
            }
            "help" -> {
                println("No help yet!")
                prompt()
            }
            else -> {
                println("What you say?! Type \"help\" for help.")
                prompt()
            }
        }
    }
}

fun main(args: Array<String>) {
    val (argsParser, cmdLineOptions) = parseArguments(*args)
    val services = connectToCordaRpc(cmdLineOptions.rpcHostAndPort, cmdLineOptions.rpcUser, cmdLineOptions.rpcPass)
    welcome()
    val daemon = if (cmdLineOptions.mockMode) MockDaemon(services, cmdLineOptions) else Daemon(services, cmdLineOptions)
    repl(daemon as Daemon, cmdLineOptions)
}