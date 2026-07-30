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
class MedicalRecordViewModel @Inject constructor(
    val database: AppDatabase,
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
            try {
                val kw = keyword?.trim()?.takeIf { it.isNotEmpty() }
                val likePattern = "%${kw ?: ""}%"
                
                // Convert Long timestamps to LocalDateTime
                val from = if (fromDate != null) {
                    java.time.Instant.ofEpochMilli(fromDate).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                } else {
                    java.time.LocalDateTime.of(1970, 1, 1, 0, 0)
                }
                
                val to = if (toDate != null) {
                    java.time.Instant.ofEpochMilli(toDate).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime().withHour(23).withMinute(59).withSecond(59)
                } else {
                    java.time.LocalDateTime.of(9999, 12, 31, 23, 59, 59)
                }
                
                val records = recordRepository.searchByMemberPaged(
                    patientId = memberId,
                    keyword = kw,
                    likePattern = likePattern,
                    from = from,
                    to = to,
                    limit = 100,
                    offset = 0
                )
                _uiState.update { it.copy(records = records, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
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
    
    suspend fun getDefaultMember(): FamilyMember? {
        return memberRepository.getDefaultMember()
    }
    
    suspend fun insertMember(member: FamilyMember): Long {
        return memberRepository.insert(member)
    }
    
    suspend fun getLatestRecord(): MedicalRecord? {
        return recordRepository.getLatestRecord()
    }
    
    suspend fun getMemberById(id: Long): FamilyMember? {
        return memberRepository.getMemberById(id)
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
