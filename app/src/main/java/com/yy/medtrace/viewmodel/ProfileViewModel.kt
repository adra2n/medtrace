package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.settings.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class ProfileViewModel @Inject constructor(
    private val database: AppDatabase,
    val premiumManager: PremiumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            database.familyMemberDao().getAllMembers().collect { members ->
                _uiState.update { it.copy(
                    defaultMember = members.find { m -> m.isDefault } ?: members.firstOrNull(),
                    memberCount = members.size
                ) }
            }
        }
        viewModelScope.launch {
            database.medicalRecordDao().getAllRecords().collect { records ->
                _uiState.update { it.copy(recordCount = records.size) }
            }
        }
        viewModelScope.launch {
            database.healthTodoDao().getAll().collect { todos ->
                _uiState.update { it.copy(todoCount = todos.size) }
            }
        }
    }
}

data class ProfileUiState(
    val defaultMember: FamilyMember? = null,
    val memberCount: Int = 0,
    val recordCount: Int = 0,
    val todoCount: Int = 0
)
