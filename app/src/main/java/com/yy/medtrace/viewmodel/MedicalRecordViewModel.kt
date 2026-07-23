package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.common.Result
import com.yy.medtrace.common.asResultWithoutLoading
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class MedicalRecordViewModel(
    private val memberRepository: MemberRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalRecordUiState())
    val uiState: StateFlow<MedicalRecordUiState> = _uiState.asStateFlow()

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

    fun loadRecords(memberId: Long, keyword: String? = null, fromDate: Long? = null, toDate: Long? = null) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Simplified: just load by member for now
            recordRepository.getRecordsByMember(memberId)
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> _uiState.update { it.copy(records = result.data, isLoading = false) }
                        is Result.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                        is Result.Loading -> {}
                    }
                }
        }
    }

    fun deleteRecord(record: com.yy.medtrace.data.model.MedicalRecord) {
        viewModelScope.launch {
            recordRepository.delete(record)
            loadRecords(_uiState.value.currentMemberId ?: 0, _uiState.value.keyword, _uiState.value.fromDate, _uiState.value.toDate)
        }
    }

    fun setFilter(memberId: Long, keyword: String?, fromDate: Long?, toDate: Long?) {
        _uiState.update { it.copy(
            currentMemberId = memberId,
            keyword = keyword,
            fromDate = fromDate,
            toDate = toDate
        ) }
        loadRecords(memberId, keyword, fromDate, toDate)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class MedicalRecordUiState(
    val members: List<com.yy.medtrace.data.model.FamilyMember> = emptyList(),
    val records: List<com.yy.medtrace.data.model.MedicalRecord> = emptyList(),
    val currentMemberId: Long = 0,
    val keyword: String? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val error: String? = null,
    val isLoading: Boolean = false
)

class MedicalRecordViewModelFactory(
    private val memberRepository: MemberRepository,
    private val recordRepository: RecordRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicalRecordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicalRecordViewModel(memberRepository, recordRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}