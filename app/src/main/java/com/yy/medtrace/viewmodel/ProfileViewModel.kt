package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
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
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            val defaultMember = database.familyMemberDao().getDefaultMember()
            val memberCount = database.familyMemberDao().count()
            _uiState.update { it.copy(
                defaultMember = defaultMember,
                memberCount = memberCount
            ) }
        }
        viewModelScope.launch {
            val recordCount = database.medicalRecordDao().count()
            _uiState.update { it.copy(recordCount = recordCount) }
        }
        viewModelScope.launch {
            val todoCount = database.healthTodoDao().count()
            _uiState.update { it.copy(todoCount = todoCount) }
        }
    }
}

data class ProfileUiState(
    val defaultMember: FamilyMember? = null,
    val memberCount: Int = 0,
    val recordCount: Int = 0,
    val todoCount: Int = 0
)
