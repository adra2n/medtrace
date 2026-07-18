package com.yy.medtrace.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "llm_settings")

class LlmSettingsStore(private val context: Context) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("llm_base_url")
        val API_KEY = stringPreferencesKey("llm_api_key")
        val MODEL = stringPreferencesKey("llm_model")
    }

    val baseUrl: Flow<String?> = context.dataStore.data.map { it[Keys.BASE_URL] }
    val apiKey: Flow<String?> = context.dataStore.data.map { it[Keys.API_KEY] }
    val model: Flow<String?> = context.dataStore.data.map { it[Keys.MODEL] }

    suspend fun getBaseUrl(): String? = context.dataStore.data.first()[Keys.BASE_URL]
    suspend fun getApiKey(): String? = context.dataStore.data.first()[Keys.API_KEY]
    suspend fun getModel(): String? = context.dataStore.data.first()[Keys.MODEL]

    suspend fun setBaseUrl(value: String) = context.dataStore.edit { it[Keys.BASE_URL] = value }
    suspend fun setApiKey(value: String) = context.dataStore.edit { it[Keys.API_KEY] = value }
    suspend fun setModel(value: String) = context.dataStore.edit { it[Keys.MODEL] = value }
}
