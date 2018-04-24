package com.r3.corda.finance.cash.issuer.daemon.mock

import com.r3.corda.finance.cash.issuer.common.types.AccountNumber
import com.r3.corda.finance.cash.issuer.common.types.NostroTransaction
import rx.Observable
import rx.Subscription
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

/**
 * For generating id numbers, etc.
 */
enum class GenerationScheme { LETTERS, NUMBERS, LETTERS_AND_NUMBERS }

private const val capitals = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val lowercase = "abcdefghijklmnopqrstuvwqyz"
private const val numbers = "0123456789"

val rng = Random()

// Generates random strings for transaction IDs and account IDs. Can use letters or numbers, or both.
fun generateRandomString(length: Long, scheme: GenerationScheme = GenerationScheme.LETTERS_AND_NUMBERS): String {
    val source = when (scheme) {
        GenerationScheme.LETTERS -> capitals + lowercase
        GenerationScheme.NUMBERS -> numbers
        else -> capitals + lowercase + numbers
    }
    return rng.ints(length, 0, source.length).toList().map(source::get).joinToString("")
}


/**
 * For generating random numbers. A ceiling can be provided
 */
// TODO: This can be improved.
// Simple function to create realistic'ish transaction amounts. For now it's OK.
fun randomAmountGenerator(ceiling: Long? = null): Long {
    fun generate(): Long {
        val selector = rng.nextInt(11)
        return when (selector) {
            0 -> rng.nextInt(1000) * 10000L         // Millions.
            in 1..4 -> rng.nextInt(1000) * 1000L    // Hundred thousands.
            in 5..7 -> rng.nextInt(1000) * 100L     // Ten thousands.
            in 8..9 -> rng.nextInt(1000) * 10L      // Thousands.
            10 -> rng.nextInt(1000).toLong()        // Hundreds.
            else -> throw IllegalStateException("Something bad happened!")
        }
    }

    // This is very hacky but does the job for now.
    return if (ceiling == null) {
        generate()
    } else {
        var amount = generate()
        while (amount > ceiling) {
            amount = generate()
        }
        amount
    }
}

/**
 * For generating random delays.
 */
// We want the delays to be mostly long but we also want a fair few short ones too.
// We can "fast forward" the generation if we can't be bothered to sit around. This caps the maximum interval at 3
// seconds and produces more "zero" delays.
fun randomDelayGenerator(fastForward: Boolean): () -> Long {
    return {
        val random = rng.nextInt(11)
        val selector = if (fastForward) random % 3 else random
        val interval = when (selector) {
            0 -> rng.nextInt(1)                     // Up to 1 second delay.            (1/10)
            1 -> rng.nextInt(3)                     // Up to 3 second delay.            (1/10)
            in 2..3 -> rng.nextInt(5)               // Up to 5 second delay.            (2/10)
            in 4..9 -> rng.nextInt(10 + 1 - 5) + 5  // Between 5 to 10 second delay.    (6/10)
            10 -> rng.nextInt(20 + 1 - 10) + 10     // Between 10 to 20 second delay.   (1/10)
            else -> throw IllegalStateException("Something bad happened!")
        }
        interval.toLong()
    }
}

/**
 * A mock contact for testing purposes.
 */
data class MockContact(val id: String, val name: String, val accountNumber: AccountNumber)

/**
 * Generates mock transaction data at random intervals, forever.
 * TODO: Mess around with publishOn() / subscribeOn() to see which schedulers perform the best.
 */
class MockTransactionGenerator(
        txGenerator: () -> NostroTransaction,
        fastForward: Boolean,
        gapGenerator: () -> Long = randomDelayGenerator(fastForward)
) {

    private var generatorSubscription: Subscription? = null
    private val transactionStream = Observable
            .fromCallable { txGenerator() }
            .delay { Observable.timer(gapGenerator(), TimeUnit.SECONDS) }
            .repeat()
    // observeOn

    fun start(numberToGenerate: Int = 0, block: (NostroTransaction) -> Unit) {
        val observable = if (numberToGenerate == 0) transactionStream else transactionStream.take(numberToGenerate)
        generatorSubscription = observable.subscribe { block(it) }
    }

    fun stop() {
        generatorSubscription?.unsubscribe()
    }

}

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
 */
//fun mockTransactionGenerator(
//        mockApi: MockApi,
//        allowWithdrawals: Boolean
//): () -> NostroTransaction {
//
//    // A hacky function that either generates semi-realistic transaction amounts for randomly selected contacts.
//    fun nextContactAndAmount(): Pair<MockContact, Long> {
//        return if (mockApi.contactBalances.isEmpty() || !allowWithdrawals) {
//            Pair(mockApi.contacts.randomOrNull()!!, randomAmountGenerator())
//        } else {
//            when (rng.nextFloat()) {
//                in 0.0f..0.6f -> Pair(mockApi.contacts.randomOrNull()!!, randomAmountGenerator())
//                in 0.6f..1.0f -> {
//                    val contact = mockApi.contactBalances.keys.toList().randomOrNull()!!
//                    val maxAmount = mockApi.contactBalances[contact]!!
//                    val amount = -randomAmountGenerator(maxAmount)
//                    Pair(contact, amount)
//                }
//                else -> throw IllegalStateException("This shouldn't happen!!")
//            }
//        }
//    }
//
//    return {
//        val (contact, amount) = nextContactAndAmount()
//
//        // Tx data.
//        val nextId = "tx_00009${generateRandomString(16)}"
//        val gbp = Currency.getInstance("GBP")
//        val type = "payport_faster_payments"
//        val description = "dummy description for now..."
//        val now = Instant.now()
//
//        val nostroTransaction = if (amount > 0L) {
//            NostroTransaction(nextId, amount, gbp, type, description, now,
//                    contact.accountNumber,
//                    destination = mockApi.accountDetails.accountNumber
//            )
//        } else {
//            NostroTransaction(nextId, amount, gbp, type, description, now,
//                    source = mockApi.accountDetails.accountNumber,
//                    destination = contact.accountNumber
//            )
//        }
//
//        // Add the balance to the map.
//        val contactBalance = mockApi.contactBalances[contact] ?: 0L
//        mockApi.contactBalances.put(contact, contactBalance + amount)
//
//        nostroTransaction
//    }
//}