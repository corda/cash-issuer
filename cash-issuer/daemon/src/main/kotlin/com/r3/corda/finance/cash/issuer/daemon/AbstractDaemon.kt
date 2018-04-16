package com.r3.corda.finance.cash.issuer.daemon

import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.loggerFor

abstract class AbstractDaemon(val services: CordaRPCOps, val cmdLineOptions: CommandLineOptions) {
    protected val autoStart: Boolean = cmdLineOptions.autoMode
    abstract val openBankingApiClients: List<OpenBankingApiClient>

    companion object {
        val logger = loggerFor<Daemon>()
    }
}