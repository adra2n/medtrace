package com.yy.medtrace.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "llm_settings")

class LlmSettingsStore(private val context: Context) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("llm_base_url")
        val MODEL = stringPreferencesKey("llm_model")
    }

    private val securePrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "llm_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    val baseUrl: Flow<String?> = context.dataStore.data.map { it[Keys.BASE_URL] }
    val model: Flow<String?> = context.dataStore.data.map { it[Keys.MODEL] }

    suspend fun getBaseUrl(): String? = context.dataStore.data.first()[Keys.BASE_URL]
    suspend fun getApiKey(): String? = securePrefs.getString("llm_api_key", null)
    suspend fun getModel(): String? = context.dataStore.data.first()[Keys.MODEL]

    suspend fun setBaseUrl(value: String) = context.dataStore.edit { it[Keys.BASE_URL] = value }
    suspend fun setApiKey(value: String) = securePrefs.edit().putString("llm_api_key", value).apply()
    suspend fun setModel(value: String) = context.dataStore.edit { it[Keys.MODEL] = value }
}
