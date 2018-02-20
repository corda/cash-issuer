package com.r3.cash.issuer.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.cash.issuer.base.types.AccountInfoWithSignature
import com.r3.cash.issuer.services.InMemoryAccountInfoService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.ProgressTracker

@StartableByRPC
class ListAllAccounts : FlowLogic<Map<CordaX500Name, AccountInfoWithSignature>>() {

    override val progressTracker: ProgressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Map<CordaX500Name, AccountInfoWithSignature> {
        val accountInfoService = serviceHub.cordaService(InMemoryAccountInfoService::class.java)
        val allAccounts = accountInfoService.getAllAccounts()
        // TODO: Here for the time being whilst this is demo'd via the node shell.
        allAccounts.forEach(::println)
        return allAccounts
    }

}