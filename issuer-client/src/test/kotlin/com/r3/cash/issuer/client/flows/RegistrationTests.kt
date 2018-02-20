package com.r3.cash.issuer.client.flows

import com.r3.cash.issuer.base.types.AccountNumber
import com.r3.cash.issuer.base.types.SortCode
import net.corda.core.crypto.Crypto
import net.corda.core.identity.CordaX500Name
import net.corda.testing.SerializationEnvironmentRule
import net.corda.testing.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.Rule
import org.junit.Test

class RegistrationTests {

    @Rule
    @JvmField
    val testSerialization = SerializationEnvironmentRule()

    @Test
    fun test() {
        val ledgerServices = MockServices(listOf("com.example.contract", "com.template"))
        val issuer = TestIdentity(CordaX500Name("Issuer", "London", "GB"))
        val alice = TestIdentity(CordaX500Name("Alice", "London", "GB"))

        val accountInformation = AccountInfo(
                accountName = "Alice Ltd",
                accountNumber = AccountNumber("12345678"),
                sortCode = SortCode("123456"),
                owner = alice.party
        )

        val serialisedAccountInformation = accountInformation.serialize()
        val hashOfserialisedAccountInformation = serialisedAccountInformation.hash.bytes
        println(serialisedAccountInformation)
        val signature = Crypto.doSign(alice.keyPair.private, hashOfserialisedAccountInformation)
        Crypto.doVerify(alice.publicKey, signature, hashOfserialisedAccountInformation)
    }

}