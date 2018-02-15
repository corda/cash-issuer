package com.r3.cc

import net.corda.client.jackson.JacksonSupport
import net.corda.core.internal.openHttpConnection
import java.io.BufferedReader
import java.net.URL

abstract class ApiClient(private val accessToken: String, private val baseUrl: String) {

    protected fun <T : Any> deserializeJson(serialisedObject: String, schema: Class<T>): T {
        return JacksonSupport.createNonRpcMapper().readValue(serialisedObject, schema)
    }

    protected fun apiRequest(method: String, endpoint: String, query: String = ""): String {
        val url = URL("$baseUrl$endpoint$query")
        val httpConnection = url.openHttpConnection()
        httpConnection.requestMethod = method
        httpConnection.setRequestProperty("Authorization", "Bearer $accessToken")
        httpConnection.setRequestProperty("User-Agent", "R3 Corda")
        return httpConnection.inputStream.bufferedReader().use(BufferedReader::readLine)
    }

}