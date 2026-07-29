package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.settings.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class SettingsViewModel @Inject constructor(
    val database: AppDatabase,
    val premiumManager: PremiumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadSettings() {
        // Mock settings data for now
        _uiState.update { it.copy(
            isDarkMode = false,
            isBackupEnabled = true
        ) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun toggleBackup() {
        _uiState.update { it.copy(isBackupEnabled = !it.isBackupEnabled) }
    }
}

data class SettingsUiState(
    val isDarkMode: Boolean = false,
    val isBackupEnabled: Boolean = false
)
