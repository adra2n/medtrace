package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.common.Result
import com.yy.medtrace.common.asResultWithoutLoading
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.AppDatabase
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
class TrendsViewModel @Inject constructor(
    val database: AppDatabase,
    private val memberRepository: MemberRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

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

    fun loadRecords(memberId: Long, days: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            recordRepository.getRecordsByMember(memberId)
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val since = java.time.LocalDateTime.now().minusDays(days)
                            _uiState.update { it.copy(
                                records = result.data.filter { r -> r.onsetTime.isAfter(since) },
                                isLoading = false
                            ) }
                        }
                        is Result.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                        is Result.Loading -> {}
                    }
                }
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class TrendsUiState(
    val members: List<com.yy.medtrace.data.model.FamilyMember> = emptyList(),
    val records: List<com.yy.medtrace.data.model.MedicalRecord> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false
)
