package com.allianz.t2i.common.contracts.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.corda.core.serialization.CordaSerializable

/**
 * Marker interface for bank account numbers.
 */
// TODO: Remove this hack at some point.
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes(
        JsonSubTypes.Type(value = UKAccountNumber::class, name = "uk"),
        JsonSubTypes.Type(value = NoAccountNumber::class, name = "none")
)
@CordaSerializable
interface AccountNumber {
    val digits: String
}

/**
 * UK bank account numbers come in sort code and account number pairs.
 */
@CordaSerializable
data class UKAccountNumber(override val digits: String) : AccountNumber {

    @JsonCreator
    constructor(sortCode: String, accountNumber: String) : this("$sortCode$accountNumber")

    val sortCode get() = digits.subSequence(0, 6)
    val accountNumber get() = digits.subSequence(6, 14)

    init {
        // Account number validation.
        require(accountNumber.length == 8) { "A UK bank account accountNumber must be eight digits long." }
        require(accountNumber.matches(Regex("[0-9]+"))) {
            "An account accountNumber must only contain the numbers zero to nine."
        }

        // Sort code validation.
        require(sortCode.length == 6) { "A UK bank sort code must be 6 digits long." }
        require(sortCode.matches(Regex("[0-9]+"))) {
            "An account accountNumber must only contain the numbers zero to nine."
        }
    }

    override fun toString() = "Sort Code: $sortCode Account Number: $accountNumber"

}

/**
 * Sometimes we don't have a bank account number.
 */
@CordaSerializable
data class NoAccountNumber(override val digits: String = "No bank account number available.") : AccountNumber