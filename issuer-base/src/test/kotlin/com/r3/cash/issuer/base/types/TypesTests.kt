package com.r3.cash.issuer.base.types

import org.junit.Test
import kotlin.test.assertFails

class TypesTests {

    @Test
    fun `Only well formed account numbers are accepted`() {
        AccountNumber("12345678")
        assertFails { AccountNumber("2468") }
        assertFails { AccountNumber("2468101214") }
        assertFails { AccountNumber("246ebc12") }
    }

    @Test
    fun `Only well formed sort codes are accepted`() {
        SortCode("001122")
        assertFails { AccountNumber("12345") }
        assertFails { AccountNumber("1234567") }
        assertFails { AccountNumber("12rg56") }
        println(SortCode("123456"))
    }

}