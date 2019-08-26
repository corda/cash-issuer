package com.allianz.t2i.issuer.daemon.mock

import com.allianz.t2i.issuer.daemon.OpenBankingApi
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.allianz.t2i.common.contracts.types.BankAccount
import com.allianz.t2i.common.contracts.types.NostroTransaction
import com.allianz.t2i.common.workflows.utilities.MockContact
import com.allianz.t2i.common.workflows.utilities.MockTransactionGenerator
import net.corda.core.contracts.Amount
import rx.Observable
import java.util.*

/**
 * A mock version of the Monzo API that is not yet that extensible. Only really used for demos. Only supports one
 * account for now. Will abstract into a mock bank later on.
 *
 * If the list of transactions is empty, then we'll generate transactions. Otherwise, they will be taken from the
 * list provided.
 */
@Suppress("Unused")
class MockMonzo(
        override val accounts: List<BankAccount> = listOf(mockMonzoAccount),
        override val transactions: MutableList<NostroTransaction> = Collections.synchronizedList<NostroTransaction>(mutableListOf()),
        override val contacts: List<MockContact> = mockMonzoCounterparties,
        fastForward: Boolean = false
) : OpenBankingApi(), MockClient {

    // Only one account per mock bank for now.
    override val account: BankAccount get() = accounts.single()
    override val transactionGenerator: MockTransactionGenerator = MockTransactionGenerator(mockTransactionGenerator(this, true), fastForward)

    override fun transactionsFeed(): Observable<List<NostroTransaction>> {
        val accountId = account.accountId
        val lastTransactionTimestamp = lastTransactions[accountId]?.plusMillis(1L)
        val transactions = transactions.takeLastWhile { it.createdAt >= lastTransactionTimestamp }
        return Observable.from(listOf(transactions))
    }

    /**
     * We need to keep a record of which contacts have depositied what balances, so that we don't withdraw more than
     * they have deposited. If they could, then the mock transactions wouldn't really make sense for testing purposes.
     */
    override val contactBalances: MutableMap<MockContact, Long> = Collections.synchronizedMap<MockContact, Long>(hashMapOf())

    override fun balance(accountId: String?): Amount<TokenType> {
        if (accountId == null) throw IllegalArgumentException("You must specify an accountId.")
        val balance = transactions.map(NostroTransaction::amount).sum()
        return Amount(balance, accounts.single().currency)
    }

    override fun startGeneratingTransactions(numberToGenerate: Int) {
        transactionGenerator.start(numberToGenerate) { transactions.add(it) }
    }

    override fun stopGeneratingTransactions() {
        transactionGenerator.stop()
    }
}
