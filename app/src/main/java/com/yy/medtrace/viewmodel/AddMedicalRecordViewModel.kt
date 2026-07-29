package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.common.Result
import com.yy.medtrace.common.asResultWithoutLoading
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class AddMedicalRecordViewModel @Inject constructor(
    val database: AppDatabase,
    private val memberRepository: MemberRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMedicalRecordUiState())
    val uiState: StateFlow<AddMedicalRecordUiState> = _uiState.asStateFlow()

    fun loadMembers() {
        viewModelScope.launch {
            memberRepository.getAllMembers()
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> _uiState.update { it.copy(members = result.data) }
                        is Result.Error -> _uiState.update { it.copy(error = result.message) }
                        is Result.Loading -> {}
                    }
                }
        }
    }

    fun saveRecord(record: com.yy.medtrace.data.model.MedicalRecord) {
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                recordRepository.insert(record)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "保存失败") }
            }
        }
    }

    fun updateRecord(record: com.yy.medtrace.data.model.MedicalRecord) {
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                recordRepository.update(record)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "更新失败") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }
}

data class AddMedicalRecordUiState(
    val members: List<com.yy.medtrace.data.model.FamilyMember> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)
