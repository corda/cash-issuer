package com.allianz.t2i.issuer.ui

import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.allianz.t2i.common.contracts.states.BankAccountState
import com.allianz.t2i.common.contracts.states.NodeTransactionState
import com.allianz.t2i.common.contracts.states.NostroTransactionState
import com.allianz.t2i.common.contracts.types.*
import com.allianz.t2i.common.contracts.types.*
    import com.allianz.t2i.common.workflows.flows.AddBankAccount
import javafx.application.Application
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.stage.StageStyle
import net.corda.client.jfx.utils.map
import net.corda.client.jfx.utils.observeOnFXThread
import net.corda.client.jfx.utils.toFXListOfStates
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.filterStatesOfType
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.utilities.NetworkHostAndPort
import tornadofx.*
import java.time.Instant
import java.util.*

/**
 * This file is a big mess but it does the job of creating a "good enough" java fx UI for the issuer.
 */

class BankAccountUiModel(
        owner: Party,
        internalAccountId: UUID,
        externalAccountId: String,
        accountName: String,
        accountNumber: AccountNumber,
        currency: TokenType,
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

class NostroTransactionUiModel(
        internalTransactionId: UUID,
        accountId: String,
        amount: Long,
        currency: TokenType,
        source: AccountNumber,
        destination: AccountNumber,
        createdAt: Instant,
        status: NostroTransactionStatus,
        type: NostroTransactionType,
        lastUpdated: Instant
) {
    val internalTransactionId by property(internalTransactionId)
    val accountId by property(accountId)
    val amount by property(amount)
    val currency by property(currency)
    val source by property(source)
    val destination by property(destination)
    val createdAt by property(createdAt)
    val status by property(status)
    val type by property(type)
    val lastUpdated by property(lastUpdated)
}

class NodeTransactionUiModel(
        transactionId: UUID,
        amount: Long,
        currency: TokenType,
        source: Party,
        notes: String,
        destination: Party,
        createdAt: Instant,
        status: NodeTransactionStatus,
        type: NodeTransactionType
) {
    val transactionId by property(transactionId)
    val amount by property(amount)
    val currency by property(currency)
    val source by property(source)
    val notes by property(notes)
    val destination by property(destination)
    val createdAt by property(createdAt)
    val status by property(status)
    val type by property(type)
}

fun NodeTransactionState.toUiModel(): NodeTransactionUiModel {
    return NodeTransactionUiModel(
            linearId.id,
            amountTransfer.quantityDelta,
            amountTransfer.token,
            amountTransfer.source,
            notes,
            amountTransfer.destination,
            createdAt,
            status,
            type
    )
}

fun BankAccountState.toUiModel(): BankAccountUiModel {
    return BankAccountUiModel(owner, linearId.id, linearId.externalId!!, accountName, accountNumber, currency, type, verified, lastUpdated)
}

fun NostroTransactionState.toUiModel(): NostroTransactionUiModel {
    return NostroTransactionUiModel(
            linearId.id,
            accountId,
            amountTransfer.quantityDelta,
            amountTransfer.token,
            amountTransfer.source,
            amountTransfer.destination,
            createdAt,
            status,
            type,
            lastUpdated
    )
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
    val _nostroTransactionsFeed = cordaRPCOps.vaultTrackBy<NostroTransactionState>()
    val _nodeTransactionsFeed = cordaRPCOps.vaultTrackBy<NodeTransactionState>()

    val nostroTransactions = FXCollections.observableArrayList(_nostroTransactionsFeed.snapshot.states.filterStatesOfType<NostroTransactionState>())
    val nodeTransactions = FXCollections.observableArrayList(_nodeTransactionsFeed.snapshot.states.filterStatesOfType<NodeTransactionState>())

    init {
        // TODO: Set all values from this loop. Update the nostro transaction table etc.
        _nostroTransactionsFeed.updates.observeOnFXThread().subscribe {
            // Update the nostro transactions list.
            nostroTransactions.removeAll(it.consumed.filterStatesOfType<NostroTransactionState>())
            nostroTransactions.addAll(it.produced.filterStatesOfType<NostroTransactionState>())

            // Update balance.
            val current = totalBankBalance.value
            val consumed = it.consumed.filterStatesOfType<NostroTransactionState>().map { it.state.data.amountTransfer.quantityDelta }.sum()
            val produced = it.produced.filterStatesOfType<NostroTransactionState>().map { it.state.data.amountTransfer.quantityDelta }.sum()
            val new = current - consumed + produced
            totalBankBalance.set(new)
        }

        _nodeTransactionsFeed.updates.observeOnFXThread().subscribe {
            // Update node transactions list.
            nodeTransactions.removeAll(it.consumed.filterStatesOfType<NodeTransactionState>())
            nodeTransactions.addAll(it.produced.filterStatesOfType<NodeTransactionState>())

            // Update total amount issued.
            val current = totalIssued.value
            val consumed = it.consumed.filterStatesOfType<NodeTransactionState>().map { it.state.data.amountTransfer.quantityDelta }.sum()
            val produced = it.produced.filterStatesOfType<NodeTransactionState>().map { it.state.data.amountTransfer.quantityDelta }.sum()
            val new = current - consumed + produced
            totalIssued.set(new)
        }
    }

    val totalBankBalance = SimpleLongProperty(_nostroTransactionsFeed.snapshot.states.filterStatesOfType<NostroTransactionState>().map { it.state.data.amountTransfer.quantityDelta }.sum())
    val totalIssued = SimpleLongProperty(_nodeTransactionsFeed.snapshot.states.filterStatesOfType<NodeTransactionState>().map { it.state.data.amountTransfer.quantityDelta }.sum())

    override val root = tabpane {
        style {
            fontSize = 10.px
        }
        tab("Information") {
            vbox {
                hbox {
                    label {
                        style {
                            fontSize = 24.px
                        }
                        text = "Total bank balances: EUR "
                    }
                    label {
                        style {
                            fontSize = 24.px
                        }
                        bind(totalBankBalance / 100)
                    }
                }
                hbox {
                    label {
                        style {
                            fontSize = 24.px
                        }
                        text = "Total amount issued: EUR "
                    }
                    label {
                        style {
                            fontSize = 24.px
                        }
                        bind(totalIssued / 100)
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
                            find<AddBankAccountView>().openModal(stageStyle = StageStyle.UTILITY)
                        }
                    }
                }
            }
        }
        tab("Nostro transactions") {
            tableview<NostroTransactionUiModel> {
                items = nostroTransactions.map { it.state.data }.transform { it.toUiModel() }
                style {
                    fontSize = 10.px
                }
                columnResizePolicy = SmartResize.POLICY
                makeIndexColumn()
                readonlyColumn("Linear Id", NostroTransactionUiModel::internalTransactionId)
                readonlyColumn("currency", NostroTransactionUiModel::currency)
                readonlyColumn("amount", NostroTransactionUiModel::amount)
                readonlyColumn("account id", NostroTransactionUiModel::accountId)
                readonlyColumn("source", NostroTransactionUiModel::source) {
                    cellFormat {
                        val source = it
                        when (source) {
                            is UKAccountNumber -> {
                                text = "${source.sortCode} ${source.accountNumber}"
                            }
                        }
                    }
                }
                readonlyColumn("destination", NostroTransactionUiModel::destination) {
                    cellFormat {
                        val destination = it
                        when (destination) {
                            is UKAccountNumber -> {
                                text = "${destination.sortCode} ${destination.accountNumber}"
                            }
                        }
                    }
                }
                readonlyColumn("Created at", NostroTransactionUiModel::createdAt)
                readonlyColumn("Last updated", NostroTransactionUiModel::lastUpdated)
                readonlyColumn("Status", NostroTransactionUiModel::status)
                readonlyColumn("Type", NostroTransactionUiModel::type)
            }
        }
        tab("Node transactions") {
            tableview<NodeTransactionUiModel> {
                items = nodeTransactions.map { it.state.data }.transform { it.toUiModel() }
                style {
                    fontSize = 10.px
                }
                columnResizePolicy = SmartResize.POLICY
                makeIndexColumn()
                readonlyColumn("Linear Id", NodeTransactionUiModel::transactionId)
                readonlyColumn("currency", NodeTransactionUiModel::currency)
                readonlyColumn("amount", NodeTransactionUiModel::amount)
                readonlyColumn("notes", NodeTransactionUiModel::notes)
                readonlyColumn("source", NodeTransactionUiModel::source)
                readonlyColumn("destination", NodeTransactionUiModel::destination)
                readonlyColumn("Created at", NodeTransactionUiModel::createdAt)
                readonlyColumn("Status", NodeTransactionUiModel::status)
                readonlyColumn("Type", NodeTransactionUiModel::type)
            }
        }
    }


}

class TestApp : App(BankAccountView::class)

class AddBankAccountView : Fragment("Cash Issuer") {
    private val model = object : ViewModel() {
        val accountId = bind { SimpleStringProperty() }
        val accountName = bind { SimpleStringProperty() }
        val sortCode = bind { SimpleStringProperty() }
        val accountNumber = bind { SimpleStringProperty() }
        val currency = bind { SimpleStringProperty() }
    }

    override val root = form {
        fieldset {
            field("Account ID") {
                textfield(model.accountId) {
                    required()
                    whenDocked { requestFocus() }
                }
            }
            field("Account Name") {
                textfield(model.accountName) {
                    required()
                    whenDocked { requestFocus() }
                }
            }
            field("Sort Code") {
                textfield(model.sortCode) {
                    required()
                    whenDocked { requestFocus() }
                }
            }
            field("Account Number") {
                textfield(model.accountNumber) {
                    required()
                    whenDocked { requestFocus() }
                }
            }
            field("Currency (EUR only)") {
                textfield(model.currency) {
                    required()
                    whenDocked { requestFocus() }
                }
            }
        }

        button("Submit") {
            isDefaultButton = true

            action {
                model.commit {
                    val ukAccountNumber = UKAccountNumber(model.sortCode.value, model.accountNumber.value)
                    val bankAccount = BankAccount(
                            accountId = model.accountId.value,
                            accountName = model.accountName.value,
                            accountNumber = ukAccountNumber,
                            currency = FiatCurrency.getInstance(model.currency.value)
                    )
                    cordaRPCOps.startFlow(::AddBankAccount, bankAccount, cordaRPCOps.nodeInfo().legalIdentities.first())
                }
            }
        }
    }
}