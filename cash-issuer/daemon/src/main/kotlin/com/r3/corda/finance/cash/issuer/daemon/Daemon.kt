package com.r3.corda.finance.cash.issuer.daemon

import com.r3.corda.finance.cash.issuer.common.flows.AddBankAccount
import com.r3.corda.finance.cash.issuer.daemon.monzo.MonzoApiClient
import com.r3.corda.finance.cash.issuer.service.flows.GetLastUpdatesByAccountId
import com.r3.corda.finance.cash.issuer.service.flows.GetNostroAccountBalances
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor

// The Daemon can start in mock or real mode.
// The Daemon can auto start or manually start via the console.

class Daemon(val services: CordaRPCOps) {

    companion object {
        private val logger = loggerFor<Daemon>()
    }

    private val openBankingApiClients = setOf(MonzoApiClient())

    init {
        /** 1. Add all bank accounts. */
        // Every time we start the daemon it queries all the available APIs for
        // bank accounts then attempts to add them. It's probably the case that
        // most/all of them have already been added but that doesn't matter.
        addBankAccounts()

        /** 2. Get all nostro account balances from the bank and the node. */
        val nodeNostroBalances = services.startFlowDynamic(GetNostroAccountBalances::class.java).returnValue
        val bankNostroBalances = openBankingApiClients.flatMap { client ->
            client.accounts.map { account ->
                Pair(account.accountId, client.balance(account.accountId))
            }
        }.toMap()

        println(nodeNostroBalances.getOrThrow())
        println(bankNostroBalances)
        // TODO: Compare the balances and print them to the console.

        /** 3. Get timestamps of the last recorded transaction for each nostro account. */
        // The daemon doesn't persist any data across restarts, so it must query the node to ascertain
        // the timestamps of the last recorded nostro transaction information. This is so the daemon
        // doesn't miss any transactions when it starts up after some downtime.
        val lastUpdatesByAccountId = services.startFlowDynamic(GetLastUpdatesByAccountId::class.java).returnValue.getOrThrow()
        if (lastUpdatesByAccountId.isNotEmpty()) {
            openBankingApiClients.forEach { client ->
                val releventAccounts = client.accounts.toSet() intersect lastUpdatesByAccountId.keys.toSet()
                // TODO: Update the map of timestamps for each api client.
            }
        }

        /** 4. Start polling the bank APIs for new transactions from the last update time. */

    }

    private fun addBankAccounts() {
        openBankingApiClients.flatMap { client -> client.accounts }.forEach {
            logger.info("Adding banking account $it.")
            services.startFlowDynamic(AddBankAccount::class.java, it)
        }
    }

}