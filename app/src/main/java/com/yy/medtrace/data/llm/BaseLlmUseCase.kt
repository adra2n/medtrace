package com.yy.medtrace.data.llm

import com.yy.medtrace.data.settings.LlmSettingsStore
import kotlinx.serialization.json.Json
import retrofit2.HttpException

abstract class BaseLlmUseCase(protected val settings: LlmSettingsStore) {
    protected val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    protected suspend fun getLlmApi(): Pair<LlmApi, String> {
        val base = settings.getBaseUrl() ?: error("LLM base URL 未设置")
        val key = settings.getApiKey() ?: error("LLM API key 未设置")
        return Pair(LlmApi.create(base.normalizeBaseUrl()), "Bearer $key")
    }

    protected suspend fun getModel(): String = settings.getModel() ?: "gpt-4o"

    protected suspend fun callApi(request: ChatRequest): String {
        val (api, auth) = getLlmApi()
        return try {
            api.chat(auth, request).choices.first().message.content
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string() ?: e.message ?: "HTTP ${e.code()}"
            throw RuntimeException("HTTP ${e.code()}: $body")
        }
    }
}
