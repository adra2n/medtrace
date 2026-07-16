package com.yy.chiyaole.data.llm

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import com.yy.chiyaole.BuildConfig

@Serializable data class Message(val role: String, val content: JsonElement)
@Serializable data class ChatRequest(val model: String, val messages: List<Message>)
@Serializable data class ChatResponse(val choices: List<Choice>)
@Serializable data class Choice(val message: ResponseMessage)
@Serializable data class ResponseMessage(val content: String)

interface LlmApi {
    @POST("chat/completions")
    suspend fun chat(@Header("Authorization") auth: String, @Body body: ChatRequest): ChatResponse

    companion object {
        fun create(baseUrl: String): LlmApi {
            val json = Json { ignoreUnknownKeys = true }
            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                )
            }
            val client = clientBuilder.build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build().create(LlmApi::class.java)
        }
    }
}
