package com.yy.chiyaole.data.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncSettingsStore(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "sync_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun getGithubToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_GITHUB_TOKEN, null)
    }

    suspend fun getEncryptPassword(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_ENCRYPT_PASSWORD, null)
    }

    suspend fun getGistId(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_GIST_ID, null)
    }

    suspend fun setGithubToken(value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_GITHUB_TOKEN, value).apply()
    }

    suspend fun setEncryptPassword(value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_ENCRYPT_PASSWORD, value).apply()
    }

    suspend fun setGistId(value: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_GIST_ID, value).apply()
    }

    companion object {
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_ENCRYPT_PASSWORD = "encrypt_password"
        private const val KEY_GIST_ID = "gist_id"
    }
}
