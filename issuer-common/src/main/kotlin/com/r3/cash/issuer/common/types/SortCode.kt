package com.r3.cash.issuer.common.types

import net.corda.core.serialization.CordaSerializable

/**
 * A type representing a UK bank account sort code.
 */
@CordaSerializable
data class SortCode(val digits: String) {

    init {
        require(digits.length == 6) { "A UK bank sort code must be 6 digits long." }
        require(digits.matches(Regex("[0-9]{6}"))) { "An account number must only contain the numbers zero to nine." }
    }

}