package com.allianz.t2i.issuer.daemon

import net.corda.client.jackson.JacksonSupport
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.Logger
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit


class OpenBankingApiFactory<T : Any>(val service: Class<T>, val config: ApiConfig, val logger: Logger) {

    private val additionalHeaders: MutableMap<String, String> = mutableMapOf()

    private fun createOkHttpClient(headers: Map<String, String>): OkHttpClient {
        return OkHttpClient().newBuilder().apply {
            // TODO: Add the capability to customise an http request.
            readTimeout(10, TimeUnit.SECONDS)
            connectTimeout(5, TimeUnit.SECONDS)

            // Add logging.
            val logger = HttpLoggingInterceptor.Logger { s -> logger.debug(s) }
            val loggingInterceptor = HttpLoggingInterceptor(logger)
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            addInterceptor(loggingInterceptor)

            // Add default header.
            addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    headers.forEach { key, value -> addHeader(key, value) }
                }
                chain.proceed(request.build())
            }

            addInterceptor(loggingInterceptor)


        }.build()
    }

    fun withAdditionalHeaders(headers: Map<String, String>): OpenBankingApiFactory<T> {
        additionalHeaders.putAll(headers)
        return this
    }

    fun build(): T {
        val headers = mapOf("Authorization" to "Bearer ${config.apiAccessToken}") + additionalHeaders
        val okHttpClient = createOkHttpClient(headers)
        val retrofit = Retrofit.Builder()
                .baseUrl(config.apiBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(JacksonSupport.createNonRpcMapper()))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()

        return retrofit.create(service)
    }

}