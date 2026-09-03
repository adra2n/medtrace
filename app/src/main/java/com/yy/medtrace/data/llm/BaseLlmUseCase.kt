package com.yy.medtrace.data.llm

import com.yy.medtrace.data.settings.AiServiceManager
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

abstract class BaseLlmUseCase(protected val aiServiceManager: AiServiceManager) {
    protected val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    protected suspend fun getLlmApi(): Pair<LlmApi, String> {
        val service = aiServiceManager.getCurrentService() ?: error("未选择 AI 服务")
        val base = service.baseUrl.ifBlank { error("LLM base URL 未设置") }
        val key = aiServiceManager.getApiKey(service.id) ?: error("LLM API key 未设置")
        return Pair(LlmApi.create(base.normalizeBaseUrl()), "Bearer $key")
    }

    protected suspend fun getModel(): String {
        val service = aiServiceManager.getCurrentService()
        return when {
            service?.model?.isNotBlank() == true -> service.model
            service?.id == "deepseek" -> "deepseek-chat"
            service?.id == "mimo" -> "mimo-chat"
            else -> "gpt-4o"
        }
    }

    protected suspend fun callApi(request: ChatRequest): String {
        val (api, auth) = getLlmApi()
        return try {
            val choices = api.chat(auth, request).choices
            if (choices.isEmpty()) throw RuntimeException("LLM 返回空响应")
            choices.first().message.content
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string() ?: e.message ?: "HTTP ${e.code()}"
            throw RuntimeException("HTTP ${e.code()}: $body")
        } catch (e: IOException) {
            throw RuntimeException("网络请求失败：${e.message ?: e.javaClass.simpleName}")
        }
    }
}
