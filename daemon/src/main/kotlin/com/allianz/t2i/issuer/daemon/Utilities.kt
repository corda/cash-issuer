package com.allianz.t2i.issuer.daemon

import net.corda.core.toFuture
import net.corda.core.utilities.getOrThrow
import retrofit2.HttpException
import rx.Observable
import rx.schedulers.Schedulers

// TODO: Make this fault tolerant.
// TODO: Deal with HTTP error codes
// Retry with exponential back-off.
// Don't bail out on error.
// Helper to convert an observable that emits one event to a future, with the work being performed on an IO thread.
fun <T : Any> Observable<T>.getOrThrow() = observeOn(Schedulers.io())
        .toFuture()
        .getOrThrow()

fun BankAccountId.truncate() = this.take(10)

fun <T : Any> wrapWithTry(block: () -> T): T {
    return try {
        block()
    } catch (e: HttpException) {
        throw RuntimeException("Creating open banking API client failed. The most likely reason is bad credentials. " +
                "Check your API key. If you don't have a monzo or starling account, then run the daemon in --mock-mode")
    }
}


