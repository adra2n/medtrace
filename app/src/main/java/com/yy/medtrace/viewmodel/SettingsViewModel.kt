package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class SettingsViewModel @Inject constructor(
    val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadSettings() {
        viewModelScope.launch {
            val userSettings = database.userSettingsDao().getUserSettings().firstOrNull() ?: UserSettings()
            _uiState.update { it.copy(
                isDarkMode = userSettings.darkMode,
                notificationSound = userSettings.enableNotificationSound,
                notificationVibration = userSettings.enableVibration,
                isBackupEnabled = true
            ) }
        }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
        saveUserSettings()
    }

    fun setNotificationSound(enabled: Boolean) {
        _uiState.update { it.copy(notificationSound = enabled) }
        saveUserSettings()
    }

    fun setNotificationVibration(enabled: Boolean) {
        _uiState.update { it.copy(notificationVibration = enabled) }
        saveUserSettings()
    }

    fun toggleBackup() {
        _uiState.update { it.copy(isBackupEnabled = !it.isBackupEnabled) }
    }

    private fun saveUserSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            val current = database.userSettingsDao().getUserSettings().firstOrNull() ?: UserSettings()
            database.userSettingsDao().insertOrUpdate(
                current.copy(
                    darkMode = state.isDarkMode,
                    enableNotificationSound = state.notificationSound,
                    enableVibration = state.notificationVibration
                )
            )
        }
    }
}

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val isBackupEnabled: Boolean = false
)
