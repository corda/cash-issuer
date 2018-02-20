package com.r3.cash.issuer.base.types

import net.corda.core.crypto.DigitalSignature
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.CordaSerializable

/**
 * A type representing a UK bank account number.
 */
@CordaSerializable
data class AccountNumber(val digits: String) {

    init {
        require(digits.length == 8) { "A UK bank account number must be eight digits long." }
        require(digits.matches(Regex("[0-9]+"))) { "An account number must only contain the numbers zero to nine." }
    }

}

/**
 * A type representing a UK bank account sort code.
 */
@CordaSerializable
data class SortCode(val digits: String) {

    init {
        require(digits.length == 6) { "A UK bank account number must be eight digits long." }
        require(digits.matches(Regex("[0-9]+"))) { "An account number must only contain the numbers zero to nine." }
    }

    override fun toString() = "${digits.subSequence(0, 2)}-${digits.subSequence(2, 4)}-${digits.subSequence(4, 6)}"

}

@CordaSerializable
data class AccountInfo(val accountName: String, val accountNumber: AccountNumber, val sortCode: SortCode, val owner: CordaX500Name)

@CordaSerializable
data class AccountInfoWithSignature(val accountInfo: AccountInfo, val signature: DigitalSignature)

@CordaSerializable
sealed class UpdateAccountInfoResponse {

    class Success : UpdateAccountInfoResponse() {
        override fun toString() = "Account info updated succeeded."
    }

    data class Failure(val message: String) : UpdateAccountInfoResponse()

}