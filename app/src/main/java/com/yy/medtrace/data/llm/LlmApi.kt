package com.yy.medtrace.data.llm

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.yy.medtrace.data.backup.HttpClientProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.ConcurrentHashMap

@Serializable data class Message(val role: String, val content: JsonElement)
@Serializable data class ChatRequest(val model: String, val messages: List<Message>)
@Serializable data class ChatResponse(val choices: List<Choice>)
@Serializable data class Choice(val message: ResponseMessage)
@Serializable data class ResponseMessage(val content: String)

interface LlmApi {
    @POST("chat/completions")
    suspend fun chat(@Header("Authorization") auth: String, @Body body: ChatRequest): ChatResponse

    companion object {
        private val cache = ConcurrentHashMap<String, LlmApi>()

        fun create(baseUrl: String): LlmApi {
            val key = baseUrl.normalizeBaseUrl()
            return cache.computeIfAbsent(key) { build(key) }
        }

        private fun build(baseUrl: String): LlmApi {
            val json = Json { ignoreUnknownKeys = true }
            val client = HttpClientProvider.createClient(
                connectTimeout = 30,
                readTimeout = 300,
                writeTimeout = 120
            )
            return Retrofit.Builder().baseUrl(baseUrl).client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build().create(LlmApi::class.java)
        }
    }
}
