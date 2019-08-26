package com.allianz.t2i.issuer.daemon.mock

import com.r3.corda.lib.tokens.money.EUR
import com.allianz.t2i.common.contracts.types.BankAccount
import com.allianz.t2i.common.contracts.types.NostroTransaction
import com.allianz.t2i.common.contracts.types.UKAccountNumber
import com.allianz.t2i.common.workflows.utilities.*
import net.corda.core.internal.randomOrNull
import java.time.Instant

interface MockClient {
    val transactions: MutableList<NostroTransaction>
    val contacts: List<MockContact>
    val transactionGenerator: MockTransactionGenerator
    val contactBalances: MutableMap<MockContact, Long>
    val account: BankAccount
    fun startGeneratingTransactions(numberToGenerate: Int)
    fun stopGeneratingTransactions()
}
val mockMonzoAccount = BankAccount(
        accountId = "acc_00009RE1DzwEupfetgm84f",
        accountName = "T2I Trust",
        accountNumber = UKAccountNumber("97784499", "040004"),
        currency = EUR
)

val mockMonzoCounterparties = listOf(
        MockContact("anonuser_e4d0fc5b4693fc16219ef7", "Roger Willis", UKAccountNumber("442200", "13371337")),
        MockContact("anonuser_qb9hcjpaem61mocujv3zh4", "David Nicol", UKAccountNumber("873456", "12345678")),
        MockContact("anonuser_x636uuqj1b913bd1mflm61", "Richard Brown", UKAccountNumber("059015", "73510753")),
        MockContact("anonuser_keu8gr5fs4qw6kj4nufy91", "Todd McDonald", UKAccountNumber("022346", "34782115")),
        MockContact("anonuser_z1oxucxi9ooep90oteb4qw", "Mike Hearn", UKAccountNumber("040040", "90143578"))
)

/**
 * This is quite a naive generator but it's useful for testing. The generation logic is as follows:
 *
 * 1. If no contacts have made a deposit then a random one is picked to make a deposit.
 * 2. If one or more contacts have made deposits, then:
 *    - if allowWithdrawals is set then there is a 40% chance that one of the depositors will withdraw an amount
 *      from the account and this amount cannot be greater than the amount they have deposited. Else, there is a 60%
 *      chance that a randomly chosen contact from the list will deposit more in the account.
 *    - if allowWithdrawals is set to false, then a random contact is chosen to make a deposit.
 * 3. Source and destination accounts are always filled in and obtained from the static data provided to this class
 *    constructor.
 * 4. All the other stuff is made up.
 *
 * It is currently tailored for monzo...
 */
fun mockTransactionGenerator(
        mockApi: MockClient,
        allowWithdrawals: Boolean
): () -> NostroTransaction {

    // A hacky function that either generates semi-realistic transaction amounts for randomly selected contacts.
    fun nextContactAndAmount(): Pair<MockContact, Long> {
        return if (mockApi.contactBalances.isEmpty() || !allowWithdrawals) {
            Pair(mockApi.contacts.randomOrNull()!!, randomAmountGenerator())
        } else {
            when (rng.nextFloat()) {
                in 0.0f..0.6f -> Pair(mockApi.contacts.randomOrNull()!!, randomAmountGenerator())
                in 0.6f..1.0f -> {
                    val contact = mockApi.contactBalances.keys.toList().randomOrNull()!!
                    val maxAmount = mockApi.contactBalances[contact]!!
                    val amount = -randomAmountGenerator(maxAmount)
                    Pair(contact, amount)
                }
                else -> throw IllegalStateException("This shouldn't happen!!")
            }
        }
    }

    return {
        val (contact, amount) = nextContactAndAmount()

        // Tx data.
        val nextId = "tx_00009${generateRandomString(16)}"
        val type = "payport_faster_payments"
        val description = "dummy description for now..."
        val now = Instant.now()

        val nostroTransaction = if (amount > 0L) {
            NostroTransaction(nextId, mockApi.account.accountId, amount, EUR, type, description, now,
                    source = contact.accountNumber,
                    destination = mockApi.account.accountNumber
            )
        } else {
            NostroTransaction(nextId, mockApi.account.accountId, amount, EUR, type, description, now,
                    source = mockApi.account.accountNumber,
                    destination = contact.accountNumber
            )
        }

        // Add the balance to the map.
        val contactBalance = mockApi.contactBalances[contact] ?: 0L
        mockApi.contactBalances.put(contact, contactBalance + amount)

        nostroTransaction
    }
}