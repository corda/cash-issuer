package com.allianz.t2i.common.workflows.utilities

import co.paralleluniverse.fibers.Suspendable
import com.allianz.t2i.common.contracts.types.AccountNumber
import com.allianz.t2i.common.contracts.types.NostroTransaction
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
@Suspendable
fun generateRandomString(length: Long, scheme: GenerationScheme = GenerationScheme.LETTERS_AND_NUMBERS): String {
    val source = when (scheme) {
        GenerationScheme.LETTERS -> capitals + lowercase
        GenerationScheme.NUMBERS -> numbers
        else -> capitals + lowercase + numbers
    }
    return rng.ints(length, 0, source.length).toList().map(source::get).joinToString("")
}


/**
 * For generating random numbers.
 * A ceiling can be provided to ensure we generate numbers < ceiling.
 */
// Simple function to create realistic'ish transaction amounts. For now it's OK.
@Suspendable
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
@Suspendable
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

    // This calls the provided function at random intervals.
    private val transactionStream = Observable
            .fromCallable { txGenerator() }
            .delay { Observable.timer(gapGenerator(), TimeUnit.SECONDS) }
            .repeat()

    fun start(numberToGenerate: Int = 0, block: (NostroTransaction) -> Unit) {
        val observable = if (numberToGenerate == 0) transactionStream else transactionStream.take(numberToGenerate)
        generatorSubscription = observable.subscribe { block(it) }
    }

    fun stop() {
        generatorSubscription?.unsubscribe()
    }
}