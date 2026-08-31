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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class FamilyViewModel @Inject constructor(
    val database: AppDatabase,
    private val memberRepository: MemberRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    fun loadMembers() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            memberRepository.getAllMembers()
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update { it.copy(members = result.data, isLoading = false) }
                            loadRecordCounts(result.data)
                        }
                        is Result.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                        is Result.Loading -> {}
                    }
                }
        }
    }

    private suspend fun loadRecordCounts(members: List<FamilyMember>) {
        if (members.isEmpty()) {
            _uiState.update { it.copy(recordCounts = emptyMap(), recentRecordsMap = emptyMap()) }
            return
        }
        val memberIds = members.map { it.id }
        val countResults = recordRepository.countByMembers(memberIds)
        val recordCounts = countResults.associate { it.patientId to it.count }
        val recentMap = mutableMapOf<Long, List<MedicalRecord>>()
        members.forEach { member ->
            recentMap[member.id] = recordRepository.getRecentRecordsByMember(member.id, 2).first()
        }
        _uiState.update { it.copy(recordCounts = recordCounts, recentRecordsMap = recentMap) }
    }

    fun addMember(member: FamilyMember) {
        viewModelScope.launch {
            memberRepository.insert(member)
            loadMembers()
        }
    }

    fun updateMember(member: FamilyMember) {
        viewModelScope.launch {
            memberRepository.update(member)
            loadMembers()
        }
    }

    fun deleteMember(memberId: Long) {
        viewModelScope.launch {
            recordRepository.reassignToUnknown(memberId)
            memberRepository.deleteById(memberId)
            loadMembers()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class FamilyUiState(
    val members: List<FamilyMember> = emptyList(),
    val recordCounts: Map<Long, Int> = emptyMap(),
    val recentRecordsMap: Map<Long, List<MedicalRecord>> = emptyMap(),
    val error: String? = null,
    val isLoading: Boolean = false
)
