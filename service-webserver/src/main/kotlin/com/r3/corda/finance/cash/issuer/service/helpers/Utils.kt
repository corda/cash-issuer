package com.r3.corda.finance.cash.issuer.service.helpers

import net.corda.core.contracts.Amount
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*


/**
 * Created by sven.lehnert@gmail.com on 18.05.18.
 *
 */

// add sumByLong functionality for Long like sumBy and sumByDouble for Int and Double
inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * @param quantityInCents in smallest currency fraction (Cents, Pennies)
 * */
fun createCurrencyAmount(quantityInCents: Long, currencyCode: String) : Amount<Currency> {
    val currency = try {
        Currency.getInstance(currencyCode)
    } catch (ex: Exception) {
        throw IllegalArgumentException("Currency code '$currencyCode' is invalid", ex)
    }

    return Amount(quantityInCents, currency)  // in Cent
}

/**
 * @param amount in BigDecimal return the quantityInCents as Long
 */
fun getLongFromBigDecimal(amount : BigDecimal) : Long {
    return (amount * BigDecimal(100).setScale(2, RoundingMode.DOWN)).longValueExact()
}

/**
 * @param amount in Long quantityInCents return BigDecimal
 */
fun getBigDecimalFromLong(amount : Long) : BigDecimal {
    return BigDecimal(amount).divide(BigDecimal(100), 2, RoundingMode.DOWN)
}
