package com.r3.corda.finance.cash.issuer.service.api.controllers

import com.google.gson.Gson
import com.r3.corda.finance.cash.issuer.common.flows.AddBankAccountFlow.AddBankAccount
import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.states.NodeTransactionState
import com.r3.corda.finance.cash.issuer.common.states.NostroTransactionState
import com.r3.corda.finance.cash.issuer.common.types.BankAccount
import com.r3.corda.finance.cash.issuer.service.api.model.CashUIModel
import com.r3.corda.finance.cash.issuer.service.api.model.NodeTransactionUiModel
import com.r3.corda.finance.cash.issuer.service.api.model.NostroTransactionUiModel
import com.r3.corda.finance.cash.issuer.service.api.model.toUiModel
import com.r3.corda.finance.cash.issuer.service.flows.GetNostroAccountBalances
import com.r3.corda.finance.cash.issuer.service.flows.VerifyBankAccount
import com.r3.corda.finance.cash.issuer.service.helpers.getBigDecimalFromLong
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.IdentityService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.getCashBalance
import net.corda.finance.contracts.getCashBalances
import net.corda.server.NodeRPCConnection
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api. All paths specified below are relative to it.
@RestController
@RequestMapping("/api") // The paths for GET and POST requests are relative to this base path.
class ServiceApiController(
        private val rpc: NodeRPCConnection,
        private val template: SimpMessagingTemplate) {

    private val proxy = rpc.proxy
    private val gson = Gson()
    private val myLegalName: CordaX500Name = proxy.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ServiceApiController>()
    }

    /**
     *  streaming information on creation/update NostroTransactionState states to a websocket
     *  The front-end can subscribe to this websocket to be notified of updates.
     *  /stomp/nostro-transaction
     */
    @SubscribeMapping("nostro-transactions")
    fun subscribeNostroTransactions() {
        val nostroTransactionsFeed = rpc.proxy.vaultTrackBy<NostroTransactionState>().updates
        nostroTransactionsFeed.subscribe { update ->
            update.produced.forEach { (state) ->
                val modelUI = state.data.toUiModel()
                template.convertAndSend("/nostro-transactions", modelUI.toJson())
            }
        }
    }

    /**
     *  streaming information on creation/update NodeTransactionState states to a websocket
     *  The front-end can subscribe to this websocket to be notified of updates.
     *  /stomp/node-transactions
     */
    @SubscribeMapping("node-transactions")
    fun subscribeNodeTransactions() {
        val nodeTransactionsFeed = rpc.proxy.vaultTrackBy<NodeTransactionState>().updates
        nodeTransactionsFeed.subscribe { update ->
            update.produced.forEach { (state) ->
                val modelUI = state.data.toUiModel()
                template.convertAndSend("/node-transactions", modelUI.toJson())
            }
        }
    }

    /** Maps a NostroTransactionUiModel to a JSON object. */
    private fun NostroTransactionUiModel.toJson(): String {
        return gson.toJson(this)
    }

    /** Maps a NodeTransactionUiModel to a JSON object. */
    private fun NodeTransactionUiModel.toJson(): String {
        return gson.toJson(this)
    }

    @GetMapping("/serviceEndpoint", produces = ["text/plain"])
    fun serviceEndpoint() = "Service GET endpoint for $myLegalName."

    /**
     * Returns the node's name.
     */
    @GetMapping("/whoami")
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GetMapping("/peers")
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }

    /**
     * Returns all bank acoounts.
     * Accessible at /api/bank-accounts
     */
    @GetMapping("/bank-accounts")
    fun getBankAccounts() = proxy.vaultQueryBy<BankAccountState>().states.map { it.state.data.toUiModel() }

    /**
     * Returns all nostro transactions.
     * Accessible at /api/nostro-transactions
     */
    @GetMapping( "/nostro-transactions")
    fun getNostroTransactions() : List<NostroTransactionUiModel> {
        return proxy.vaultQueryBy<NostroTransactionState>().states.map { it.state.data.toUiModel() }
    }

    /**
     * Returns all node transactions.
     * Accessible at /api/node-transactions
     */
    @GetMapping("/node-transactions")
    fun getNNodeTransactions() : List<NodeTransactionUiModel> {
        return proxy.vaultQueryBy<NodeTransactionState>().states.map { it.state.data.toUiModel() }
    }

    /**
     * Returns current unspented cash.
     * Accessible at /api/cash
     */
    @GetMapping("/cash")
    fun cash() : List<CashUIModel> {
        return proxy.vaultQueryBy<Cash.State>(QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)).states.map { it.state.data.toUiModel() }
    }

    /**
     * Returns current cash balance for given Currency (ISO Code)
     * Accessible at /api/service/cash/sum/{iso}
     */
    @GetMapping("/cash/sum/{iso}", produces = ["text/plain"])
    fun cashSum(@PathVariable("iso") iso: String) : Long {
        return proxy.getCashBalance(Currency.getInstance(iso)).quantity
    }


    /**
     * Returns current cash balances.
     * Accessible at /api/cash-balances
     */
    @GetMapping("/cash-balances")
    fun getCashBalances() : Map<String, BigDecimal> {
        val cashBalances: Map<Currency, Amount<Currency>> = proxy.getCashBalances()
        val resultMap = mutableMapOf<String, BigDecimal>()
        cashBalances.map { (k,v) -> resultMap.put(k.toString(), getBigDecimalFromLong(v.quantity)) }
        return resultMap
    }

    /**
     * Initiates a flow to verify a bank account.
     * flow start AddBankAccount bankAccount: { accountId: "12345", accountName: "Roger's Account",
     * accountNumber: { sortCode: "442200" , accountNumber: "13371337", type: "uk" }, currency: "GBP" }, verifier: Issuer
     */
    @PutMapping("/add-account")
    fun addBankAccount(@RequestBody bankAccount: BankAccount): ResponseEntity<String> {
        return try {
            val signedTx = proxy.startTrackedFlow(::AddBankAccount, bankAccount, proxy.nodeInfo().legalIdentities.first()).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().eTag(ex.message!!).build()
        }
    }

    /**
     * Initiates a flow to verify a bank account.
     */
    @PutMapping("/verify-account")
    fun verifyBankAccount(@RequestParam("internalAccountId") internalAccountId: String?): ResponseEntity<String> {
        if (internalAccountId.isNullOrEmpty()) {
            return ResponseEntity.badRequest().eTag("Query parameter 'internalAccountId' missing or has wrong format.\n").build()
        }

        return try {
            val signedTx = proxy.startTrackedFlow(::VerifyBankAccount, UniqueIdentifier.fromString(internalAccountId!!)).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().eTag(ex.message!!).build()
        }
    }

    @GetMapping("/nostro-balances")
    fun getNostroAccountBalances(): ResponseEntity<Map<String, Long>> {
        return try {
            val balances = proxy.startFlow(::GetNostroAccountBalances).returnValue.getOrThrow()
            ResponseEntity.ok(balances)
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().eTag(ex.message!!).build()
        }
    }

    //TODO GetLastUpdatesByAccountId, MoveCash?

}