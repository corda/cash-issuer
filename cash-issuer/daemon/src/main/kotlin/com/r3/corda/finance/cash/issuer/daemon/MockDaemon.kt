package com.r3.corda.finance.cash.issuer.daemon

import net.corda.core.messaging.CordaRPCOps

class MockDaemon(services: CordaRPCOps, options: CommandLineOptions) : AbstractDaemon(services, options) {
    override val openBankingApiClients: List<OpenBankingApiClient> get() = listOf()
}