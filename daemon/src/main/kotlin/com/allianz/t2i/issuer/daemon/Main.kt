package com.allianz.t2i.issuer.daemon

import com.allianz.t2i.issuer.daemon.mock.MockMonzo
import com.r3.corda.lib.tokens.money.EUR
import com.allianz.t2i.common.contracts.types.NostroTransaction
import com.allianz.t2i.common.contracts.types.UKAccountNumber
import com.allianz.t2i.common.workflows.utilities.generateRandomString
import joptsimple.OptionException
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.RPCException
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort.Companion.parse
import java.time.Instant
import java.util.*
import kotlin.system.exitProcess

private fun parseArguments(vararg args: String): CommandLineOptions {
    val argsParser = ArgsParser()
    return try {
        argsParser.parse(*args)
    } catch (ex: OptionException) {
        println("Invalid command line arguments: ${ex.message}")
        argsParser.printHelp(System.out)
        exitProcess(1)
    }
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

private fun prompt() = print("> ")

private fun manual(daemon: AbstractDaemon, cmdLineOptions: CommandLineOptions, scanner: Scanner) {
    if (!cmdLineOptions.mockMode) {
        println("Manually adding transaction can only be done in mock mode.")
        return
    }

    if (daemon.started) {
        println("Stopping auto generation of transactions.")
        daemon.stop()
    }

    println("Manual transaction entry starting...")

    val txId = "tx_00009${generateRandomString(16)}"
    val accountId = "acc_00009RE1DzwEupfetgm84f"
    val type = "payport_faster_payments"
    val now = Instant.now()

    println("Enter in description (This is the redemption code!)")
    prompt()
    val description = scanner.nextLine()

    println("Enter the source account number")
    prompt()
    val source = UKAccountNumber(scanner.nextLine())

    println("Enter the destination account number")
    prompt()
    val destination = UKAccountNumber(scanner.nextLine())

    require(source != destination) { "Source and destination account cannot be the same." }

    println("Enter the amount")
    prompt()
    val amount = scanner.nextLine().toLong()

    val tx = NostroTransaction(txId, accountId, amount, EUR, type, description, now, source, destination)

    val monzo = daemon.openBankingApiClients.single { it is MockMonzo } as MockMonzo
    monzo.transactions.add(tx)
    // Grab the new transaction from the list.
    daemon.start()
    daemon.stop()
}

private fun repl(daemon: AbstractDaemon, cmdLineOptions: CommandLineOptions) {
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
            "manual" -> {
                manual(daemon, cmdLineOptions, scanner)
                prompt()
            }
            "quit" -> {
                println("Bye bye!")
                exitProcess(0)
            }
            "exit" -> {
                println("Bye bye!")
                exitProcess(0)
            }
            "help" -> {
                println("start - start polling for transactions.")
                println("manual for manually adding a transaction - mock mode only")
                println("stop - stop polling for transactions.")
                println("exit/quit - Exit process.")
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
    val cmdLineOptions = parseArguments(*args)
    val services = try {
        connectToCordaRpc(cmdLineOptions.rpcHostAndPort, cmdLineOptions.rpcUser, cmdLineOptions.rpcPass)
    } catch (e: RPCException) {
        throw RuntimeException("Could not connect to RPC client on host and post: ${cmdLineOptions.rpcHostAndPort}.")
    }
    welcome()
    if (cmdLineOptions.mockMode) {
        repl(MockDaemon(services, cmdLineOptions), cmdLineOptions)
    } else {
        repl(Daemon(services, cmdLineOptions), cmdLineOptions)
    }
}