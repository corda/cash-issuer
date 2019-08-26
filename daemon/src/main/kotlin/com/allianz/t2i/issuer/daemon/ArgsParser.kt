package com.allianz.t2i.issuer.daemon

import joptsimple.OptionParser
import java.io.PrintStream
import java.time.Instant

class ArgsParser {
    private val optionParser = OptionParser()

    private val rpcHostPortArg = optionParser.accepts("host-port", "The hostname and port of the Issuer node.").withRequiredArg()
    private val rpcUserArg = optionParser.accepts("rpcUser", "Corda RPC username.").withRequiredArg().defaultsTo("user1")
    private val rpcPassArg = optionParser.accepts("rpcPass", "CordaRPC password.").withRequiredArg().defaultsTo("test")
    private val mockModeArg = optionParser.accepts("mock-mode", "Run the daemon in mockMode mode.")
    private val autoModeArg = optionParser.accepts("auto-mode", "Run the daemon in automatic mode.")
    private val startFromArg = optionParser.accepts("start-from", "Run the daemon in automatic mode.").withOptionalArg().defaultsTo(Instant.now().toEpochMilli().toString())

    fun parse(vararg args: String): CommandLineOptions {
        val optionSet = optionParser.parse(*args)
        return CommandLineOptions(
                rpcHostAndPort = optionSet.valueOf(rpcHostPortArg),
                rpcUser = optionSet.valueOf(rpcUserArg),
                rpcPass = optionSet.valueOf(rpcPassArg),
                mockMode = optionSet.has(mockModeArg),
                autoMode = optionSet.has(autoModeArg),
                startFrom = Instant.ofEpochMilli(optionSet.valueOf(startFromArg).toLong())
        )
    }

    fun printHelp(sink: PrintStream) = optionParser.printHelpOn(sink)
}

data class CommandLineOptions(
        val rpcHostAndPort: String,
        val rpcUser: String,
        val rpcPass: String,
        val mockMode: Boolean,
        val autoMode: Boolean,
        val startFrom: Instant?
)