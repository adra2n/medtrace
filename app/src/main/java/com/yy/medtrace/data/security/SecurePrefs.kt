package com.yy.medtrace.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurePrefs {
    private const val FILE_NAME = "chiyaole_secure_prefs"
    const val PIN_HASH = "pin_hash"

    @Volatile
    private var cachedInstance: EncryptedSharedPreferences? = null

    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    @Synchronized
    fun get(context: Context): EncryptedSharedPreferences {
        cachedInstance?.let { return it }
        val instance = EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey(context),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
        cachedInstance = instance
        return instance
    }
}
