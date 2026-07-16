package com.yy.chiyaole.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.securityDataStore by preferencesDataStore(name = "security_settings")

class SecuritySettingsStore(private val context: Context) {
    private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    private val AUTO_LOCK_SECONDS = intPreferencesKey("auto_lock_seconds")
    private val SECURE_SCREEN = booleanPreferencesKey("secure_screen")

    suspend fun getAppLockEnabled(): Boolean =
        context.securityDataStore.data.map { it[APP_LOCK_ENABLED] ?: false }.first()

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.securityDataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    suspend fun getAutoLockSeconds(): Int =
        context.securityDataStore.data.map { it[AUTO_LOCK_SECONDS] ?: 0 }.first()

    suspend fun setAutoLockSeconds(seconds: Int) {
        context.securityDataStore.edit { it[AUTO_LOCK_SECONDS] = seconds }
    }

    suspend fun getSecureScreen(): Boolean =
        context.securityDataStore.data.map { it[SECURE_SCREEN] ?: false }.first()

    suspend fun setSecureScreen(enabled: Boolean) {
        context.securityDataStore.edit { it[SECURE_SCREEN] = enabled }
    }
}
