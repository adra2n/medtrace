package com.yy.medtrace.data.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivacyConsentStore(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "privacy_consent_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun isGranted(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(KEY_GRANTED, false)
    }

    suspend fun setGranted() = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_GRANTED, true).apply()
    }

    companion object {
        private const val KEY_GRANTED = "privacy_consent_granted"
    }
}
