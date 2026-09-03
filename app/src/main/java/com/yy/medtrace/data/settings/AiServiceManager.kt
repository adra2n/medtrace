package com.yy.medtrace.data.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AI服务数据类
 */
@Serializable
data class AiService(
    val id: String,
    val name: String,
    val baseUrl: String,
    val model: String = "",
    val isBuiltIn: Boolean = false
)

/**
 * AI服务配置管理
 * 支持内置服务（DeepSeek、Mimo）和用户自定义服务
 */
@Singleton
class AiServiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _services = MutableStateFlow(loadServices())
    val services: StateFlow<List<AiService>> = _services.asStateFlow()

    private val _currentServiceId = MutableStateFlow(loadCurrentServiceId())
    val currentServiceId: StateFlow<String> = _currentServiceId.asStateFlow()

    companion object {
        private const val PREFS_NAME = "ai_service_prefs"
        private const val KEY_SERVICES = "services"
        private const val KEY_CURRENT_SERVICE = "current_service"
        private const val KEY_API_KEY_PREFIX = "api_key_"

        // 内置服务
        val BUILT_IN_SERVICES = listOf(
            AiService("deepseek", "DeepSeek", "https://api.deepseek.com", "deepseek-chat", true),
            AiService("mimo", "Mimo", "https://api.mimo.com", "mimo-chat", true)
        )
    }

    /**
     * 获取所有服务（内置 + 自定义）
     */
    fun getAllServices(): List<AiService> {
        return BUILT_IN_SERVICES + _services.value.filter { !it.isBuiltIn }
    }

    /**
     * 获取当前选中的服务
     */
    fun getCurrentService(): AiService? {
        val currentId = _currentServiceId.value
        return getAllServices().find { it.id == currentId }
    }

    /**
     * 获取当前服务的API Key
     */
    fun getApiKey(serviceId: String): String? {
        return prefs.getString(KEY_API_KEY_PREFIX + serviceId, null)
    }

    /**
     * 设置API Key
     */
    fun setApiKey(serviceId: String, apiKey: String) {
        prefs.edit().putString(KEY_API_KEY_PREFIX + serviceId, apiKey).apply()
    }

    /**
     * 选择服务
     */
    fun selectService(serviceId: String) {
        prefs.edit().putString(KEY_CURRENT_SERVICE, serviceId).apply()
        _currentServiceId.value = serviceId
    }

    /**
     * 添加或更新自定义服务（按 id 覆盖）
     */
    fun addCustomService(service: AiService) {
        val currentServices = _services.value.toMutableList()
        val index = currentServices.indexOfFirst { it.id == service.id }
        if (index >= 0) {
            currentServices[index] = service.copy(isBuiltIn = false)
        } else {
            currentServices.add(service.copy(isBuiltIn = false))
        }
        _services.value = currentServices
        saveServices(currentServices)
    }

    /**
     * 删除自定义服务
     */
    fun removeCustomService(serviceId: String) {
        val currentServices = _services.value.toMutableList()
        currentServices.removeAll { it.id == serviceId && !it.isBuiltIn }
        _services.value = currentServices
        saveServices(currentServices)

        // 同时清除该服务保存的 API Key
        prefs.edit().remove(KEY_API_KEY_PREFIX + serviceId).apply()

        // 如果删除的是当前服务，切换到第一个内置服务
        if (_currentServiceId.value == serviceId) {
            selectService(BUILT_IN_SERVICES.first().id)
        }
    }

    /**
     * 检查是否有自定义服务
     */
    fun hasCustomServices(): Boolean {
        return _services.value.any { !it.isBuiltIn }
    }

    /**
     * 从存储中加载自定义服务
     */
    private fun loadServices(): List<AiService> {
        val json = prefs.getString(KEY_SERVICES, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<AiService>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存自定义服务到存储
     */
    private fun saveServices(services: List<AiService>) {
        val customServices = services.filter { !it.isBuiltIn }
        prefs.edit().putString(KEY_SERVICES, Json.encodeToString(customServices)).apply()
    }

    /**
     * 从存储中加载当前服务ID
     */
    private fun loadCurrentServiceId(): String {
        return prefs.getString(KEY_CURRENT_SERVICE, BUILT_IN_SERVICES.first().id)
            ?: BUILT_IN_SERVICES.first().id
    }
}
