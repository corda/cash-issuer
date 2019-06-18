package com.r3.corda.sdk.issuer.common.contracts.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.r3.corda.lib.tokens.money.FiatCurrency

class FiatCurrencySerializer : JsonSerializer<FiatCurrency>() {
    override fun serialize(value: FiatCurrency, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.tokenIdentifier)
    }
}