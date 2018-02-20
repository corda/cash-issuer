package com.r3.cash.issuer.daemon

import com.r3.cash.issuer.daemon.mockbank.Counterparty
import com.r3.cash.issuer.daemon.mockbank.MockTransaction
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

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

// TODO: This can be improved.
// Simple function to create realistic'ish transaction amounts. For now it's OK.
fun randomAmountGenerator(): Long {
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

// We want the delays to be mostly long but we also want a fair few short ones too.
// We can "fast forward" the generation if we can't be bothered to sit around. This caps the maximum interval at 3
// seconds and produces much more "zero" delays.
fun randomDelayGenerator(fastForward: Boolean = false): () -> Long {
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

val counterparties = listOf(
        Counterparty("Roger Willis", "12538727", "442200"),
        Counterparty("David Nicol", "41558501", "873456"),
        Counterparty("Joel Dudley", "73510753", "059015"),
        Counterparty("Richard Geeen", "34782115", "022346"),
        Counterparty("Cais Manai", "90143578", "040040")
)

// Generates mock transaction data at random intervals.
// TODO: Mess around with publishOn() / subscribeOn() to see which schedulers perform the best.
class MockTransactionGenerator(txGenerator: () -> MockTransaction, gapGenerator: () -> Long = randomDelayGenerator()) {

    private val transactionStream = Observable
            .fromCallable { txGenerator() }
            .subscribeOn(Schedulers.io())
            .delay { Observable.timer(gapGenerator(), TimeUnit.SECONDS) }
            .repeat()

    fun start(amount: Int = 0): Observable<MockTransaction> {
        return if (amount == 0) {
            transactionStream
        } else {
            transactionStream.take(amount)
        }
    }

}