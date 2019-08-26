package com.allianz.t2i.common.contracts.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.r3.corda.lib.tokens.contracts.types.TokenType

class TokenTypeSerializer : JsonSerializer<TokenType>() {
    override fun serialize(value: TokenType, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.tokenIdentifier)
    }
}