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
class MemberDetailViewModel @Inject constructor(
    val database: AppDatabase,
    private val memberRepository: MemberRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    fun loadMember(memberId: Long) {
        viewModelScope.launch {
            try {
                val member = memberRepository.getMemberById(memberId)
                _uiState.update { it.copy(member = member) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "加载成员失败") }
            }
        }
    }

    fun loadRecords(memberId: Long) {
        viewModelScope.launch {
            recordRepository.getRecordsByMember(memberId)
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> _uiState.update { it.copy(records = result.data) }
                        is Result.Error -> _uiState.update { it.copy(error = result.message) }
                        is Result.Loading -> {}
                    }
                }
        }
    }

    fun loadAll(memberId: Long) {
        loadMember(memberId)
        loadRecords(memberId)
    }

    fun updateMember(member: com.yy.medtrace.data.model.FamilyMember) {
        viewModelScope.launch {
            memberRepository.update(member)
            _uiState.update { it.copy(member = member) }
        }
    }

    fun deleteRecord(record: com.yy.medtrace.data.model.MedicalRecord) {
        viewModelScope.launch {
            recordRepository.delete(record)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class MemberDetailUiState(
    val member: com.yy.medtrace.data.model.FamilyMember? = null,
    val records: List<com.yy.medtrace.data.model.MedicalRecord> = emptyList(),
    val error: String? = null
)
