//package com.r3.corda.finance.cash.issuer.client.services
//
//import com.r3.corda.finance.cash.issuer.common.flows.AddBankAccount
//import io.bluebank.braid.corda.BraidConfig
//import net.corda.core.node.AppServiceHub
//import net.corda.core.node.services.CordaService
//import net.corda.core.serialization.SingletonSerializeAsToken
//
//@CordaService
//class BraidService(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
//    /**
//     * config file name based on the node legal identity
//     */
//    private val configFileName: String
//        get() {
//            val name = serviceHub.myInfo.legalIdentities.first().name.organisation
//            return "braid-$name.json"
//        }
//
//    init {
//        BraidConfig.fromResource(configFileName)?.bootstrap()
//    }
//
//    private fun BraidConfig.bootstrap() {
//        this.withFlow("addBankAccount", AddBankAccount::class).bootstrapBraid(serviceHub)
//    }
//}
