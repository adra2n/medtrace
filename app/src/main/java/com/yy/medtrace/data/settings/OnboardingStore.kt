package com.yy.medtrace.data.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OnboardingStore(private val context: Context) {
    private val prefs by lazy {
        createPrefs()
    }

    private fun createPrefs(): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return try {
            EncryptedSharedPreferences.create(
                context,
                "onboarding_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        } catch (e: Exception) {
            context.deleteSharedPreferences("onboarding_prefs")
            EncryptedSharedPreferences.create(
                context,
                "onboarding_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
        }
    }

    suspend fun isDone(): Boolean = withContext(Dispatchers.IO) {
        runCatching { prefs.getBoolean(KEY_DONE, false) }.getOrDefault(false)
    }

    suspend fun setDone() = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    companion object {
        private const val KEY_DONE = "onboarding_done"
    }
}
