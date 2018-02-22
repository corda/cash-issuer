package com.r3.cash.issuer.common.types

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class AccountNumber(val digits: String) {

    init {
        require(digits.length == 8) { "A UK bank account number must be eight digits long." }
        require(digits.matches(Regex("[0-9]+"))) { "An account number must only contain the numbers zero to nine." }
    }

}

