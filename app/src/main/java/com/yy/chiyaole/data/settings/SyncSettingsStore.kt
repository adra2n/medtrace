package com.yy.chiyaole.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sync_settings")

class SyncSettingsStore(private val context: Context) {
    private object Keys {
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val ENCRYPT_PASSWORD = stringPreferencesKey("encrypt_password")
        val GIST_ID = stringPreferencesKey("gist_id")
    }

    val githubToken: Flow<String?> = context.dataStore.data.map { it[Keys.GITHUB_TOKEN] }
    val encryptPassword: Flow<String?> = context.dataStore.data.map { it[Keys.ENCRYPT_PASSWORD] }
    val gistId: Flow<String?> = context.dataStore.data.map { it[Keys.GIST_ID] }

    suspend fun getGithubToken(): String? = context.dataStore.data.first()[Keys.GITHUB_TOKEN]
    suspend fun getEncryptPassword(): String? = context.dataStore.data.first()[Keys.ENCRYPT_PASSWORD]
    suspend fun getGistId(): String? = context.dataStore.data.first()[Keys.GIST_ID]

    suspend fun setGithubToken(value: String) = context.dataStore.edit { it[Keys.GITHUB_TOKEN] = value }
    suspend fun setEncryptPassword(value: String) = context.dataStore.edit { it[Keys.ENCRYPT_PASSWORD] = value }
    suspend fun setGistId(value: String) = context.dataStore.edit { it[Keys.GIST_ID] = value }
}
