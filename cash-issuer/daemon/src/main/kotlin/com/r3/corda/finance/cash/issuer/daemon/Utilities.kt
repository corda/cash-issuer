package com.r3.corda.finance.cash.issuer.daemon

import net.corda.core.toFuture
import net.corda.core.utilities.getOrThrow
import rx.Observable
import rx.schedulers.Schedulers

// TODO: Make this fault tolerant.
// Retry with exponential back-off.
// Don't bail out on error.
// Helper to convert an observable that emits one event to a future, with the work being performed on an IO thread.
fun <T : Any> Observable<T>.getOrThrow() = observeOn(Schedulers.io())
        .doOnError {
            println(it.message)
            it.printStackTrace()
        }
        .toFuture()
        .getOrThrow()!!

fun BankAccountId.truncate() = "${this.take(16)}..."

