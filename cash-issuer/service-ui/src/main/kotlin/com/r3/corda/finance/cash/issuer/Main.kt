package com.r3.corda.finance.cash.issuer

import com.r3.corda.finance.cash.issuer.common.states.BankAccountState
import com.r3.corda.finance.cash.issuer.common.states.NostroTransactionState
import com.r3.corda.finance.cash.issuer.common.types.AccountNumber
import com.r3.corda.finance.cash.issuer.common.types.BankAccountType
import com.r3.corda.finance.cash.issuer.common.types.UKAccountNumber
import javafx.application.Application
import javafx.beans.property.SimpleLongProperty
import javafx.collections.ObservableList
import net.corda.client.jfx.utils.map
import net.corda.client.jfx.utils.observeOnFXThread
import net.corda.client.jfx.utils.toFXListOfStates
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.filterStatesOfType
import net.corda.core.identity.Party
import net.corda.core.internal.toMultiMap
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.utilities.NetworkHostAndPort
import tornadofx.*
import java.time.Instant
import java.util.*

class BankAccountUiModel(
        owner: Party,
        internalAccountId: UUID,
        externalAccountId: String,
        accountName: String,
        accountNumber: AccountNumber,
        currency: Currency,
        type: BankAccountType,
        verified: Boolean,
        lastUpdated: Instant
) {
    val owner by property(owner)
    val internalAccountId by property(internalAccountId)
    val externalAccountId by property(externalAccountId)
    val accountName by property(accountName)
    val accountNumber by property(accountNumber)
    val currency by property(currency)
    val type by property(type)
    var verified by property(verified)
    val lastUpdated by property(lastUpdated)
    fun verifiedProperty() = getProperty(BankAccountUiModel::verified)
}

fun BankAccountState.toUiModel(): BankAccountUiModel {
    return BankAccountUiModel(owner, linearId.id, linearId.externalId!!, accountName, accountNumber, currency, type, verified, lastUpdated)
}

fun <T : ContractState, U : Any> ObservableList<T>.transform(block: (T) -> U) = map { block(it) }

private fun connectToCordaRpc(hostAndPort: String, username: String, password: String): CordaRPCOps {
    println("Connecting to Issuer node $hostAndPort.")
    val nodeAddress = NetworkHostAndPort.parse(hostAndPort)
    val client = CordaRPCClient(nodeAddress)
    val cordaRpcOps = client.start(username, password).proxy
    println("Connected!")
    return cordaRpcOps
}

val cordaRPCOps = connectToCordaRpc("localhost:10006", "user1", "test")

fun main(args: Array<String>) {

    Application.launch(TestApp::class.java, *args)
}

class BankAccountView : View("Cash Issuer") {

    val bankAccountFeed = cordaRPCOps.vaultTrackBy<BankAccountState>().toFXListOfStates().transform { it.toUiModel() }
    val nostroTransactionFeed = cordaRPCOps.vaultTrackBy<NostroTransactionState>().toFXListOfStates()

    init {
        // TODO: Set all values from this loop. Update the nostro transaction table etc.
        cordaRPCOps.vaultTrackBy<NostroTransactionState>().updates.observeOnFXThread().subscribe {
            val current = totalBalance.value
            val consumed = it.consumed.filterStatesOfType<NostroTransactionState>().map { it.state.data.amountTransfer.quantityDelta }.sum()
            val produced = it.produced.filterStatesOfType<NostroTransactionState>().map { it.state.data.amountTransfer.quantityDelta }.sum()
            val new = current - consumed + produced
            totalBalance.set(new)
        }
    }

    val totalBalance = SimpleLongProperty(cordaRPCOps.vaultTrackBy<NostroTransactionState>().snapshot.states.map {
        it.state.data.amountTransfer.quantityDelta
    }.sum())

    val balancePerAccount = cordaRPCOps.vaultTrackBy<NostroTransactionState>().snapshot.states.map {
        it.state.data.accountId to it.state.data.amountTransfer.quantityDelta
    }.toMultiMap().mapValues { it.value.sum() }

    override val root = tabpane {
        tab("Information") {
            vbox {
                hbox {
                    label {
                        text = "Total bank balance: GBP "
                    }
                    label {
                        bind(totalBalance / 100L)
                    }
                }
                // list of account balances.
                // list of amount currently issued.
                // how much the account is over collateralised.
            }
        }
        tab("Bank accounts") {
            borderpane {
                center {
                    tableview<BankAccountUiModel> {
                        items = bankAccountFeed
                        style {
                            fontSize = 10.px
                        }
                        columnResizePolicy = SmartResize.POLICY
                        makeIndexColumn()
                        readonlyColumn("Internal Id", BankAccountUiModel::internalAccountId)
                        readonlyColumn("Account Name", BankAccountUiModel::accountName)
                        readonlyColumn("Currency", BankAccountUiModel::currency)
                        readonlyColumn("Account Number", BankAccountUiModel::accountNumber) {
                            cellFormat {
                                when (it) {
                                    is UKAccountNumber -> {
                                        text = "${it.sortCode} ${it.accountNumber}"
                                    }
                                }
                            }
                        }
                        column("Verified", BankAccountUiModel::verified).useCheckbox(true)
                    }
                }
                bottom {
                    toolbar {
                        // Click to add an update to the first entry
                        button("Add bank account").action {
                            TODO()
                        }
                    }
                }
            }
        }
        tab("Nostro transactions") {
            tableview<NostroTransactionState> {
                items = nostroTransactionFeed
                style {
                    fontSize = 10.px
                }
                columnResizePolicy = SmartResize.POLICY
                makeIndexColumn()
                readonlyColumn("Linear Id", NostroTransactionState::linearId) { cellFormat { it.id } }
                readonlyColumn("amount", NostroTransactionState::amountTransfer) { cellFormat { it.quantityDelta } }
                readonlyColumn("currency", NostroTransactionState::amountTransfer) { cellFormat { it.token } }
                readonlyColumn("source", NostroTransactionState::amountTransfer) {
                    cellFormat {
                        val source = it.source
                        when (source) {
                            is UKAccountNumber -> {
                                text = "${source.sortCode} ${source.accountNumber}"
                            }
                        }
                    }
                }
                readonlyColumn("destination", NostroTransactionState::amountTransfer) {
                    cellFormat {
                        val destination = it.destination
                        when (destination) {
                            is UKAccountNumber -> {
                                text = "${destination.sortCode} ${destination.accountNumber}"
                            }
                        }
                    }
                }
                readonlyColumn("Last updated", NostroTransactionState::lastUpdated)
                readonlyColumn("Status", NostroTransactionState::status)
                readonlyColumn("Type", NostroTransactionState::type)
            }
        }
        tab("Node transactions") {
            // List of node transactions with their status. It's a read only table.
        }
    }


}

class TestApp : App(BankAccountView::class)