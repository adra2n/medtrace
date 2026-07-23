package com.yy.medtrace.data.backup

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.yy.medtrace.BuildConfig
import java.util.concurrent.TimeUnit

object HttpClientProvider {
    val client: OkHttpClient by lazy {
        createClient()
    }

    fun createClient(
        connectTimeout: Long = 30,
        readTimeout: Long = 30,
        writeTimeout: Long = 30
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        return builder.build()
    }
}