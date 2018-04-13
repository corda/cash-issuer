package com.r3.corda.finance.cash.issuer.daemon

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import net.corda.client.jackson.JacksonSupport
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

class RestClientFactory<T : Any>(val service: Class<T>, configFile: String = "accounts.conf") {

    private val additionalHeaders: MutableMap<String, String> = mutableMapOf()

    private val config: Config = ConfigFactory.parseResources(configFile).getConfig(service.simpleName.toLowerCase())
    private val apiBaseUrl: String = config.getString("apiBaseUrl") + config.getString("apiVersion")
    private val apiAccessToken: String = config.getString("apiAccessToken")

    private fun createOkHttpClient(headers: Map<String, String>): OkHttpClient {
        val builder = OkHttpClient().newBuilder()
        // TODO: Add the capability to customise an http request.
        builder.readTimeout(10, TimeUnit.SECONDS)
        builder.connectTimeout(5, TimeUnit.SECONDS)

        builder.addInterceptor { chain ->
            val request = chain.request().newBuilder().apply {
                headers.forEach { key, value -> addHeader(key, value) }
            }
            chain.proceed(request.build())
        }

        return builder.build()
    }

    fun withAdditionalHeaders(headers: Map<String, String>): RestClientFactory<T> {
        additionalHeaders.putAll(headers)
        return this
    }

    fun build(): T {
        val headers = mapOf("Authorization" to "Bearer $apiAccessToken") + additionalHeaders
        val okHttpClient = createOkHttpClient(headers)
        val retrofit = Retrofit.Builder()
                .baseUrl(apiBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(JacksonSupport.createNonRpcMapper()))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()

        return retrofit.create(service)
    }

}