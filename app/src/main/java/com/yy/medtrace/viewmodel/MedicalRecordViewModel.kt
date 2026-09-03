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
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class MedicalRecordViewModel @Inject constructor(
    val database: AppDatabase,
    private val memberRepository: MemberRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {

    /** 单页条数。分页为增量追加，记录再多也不会被截断。 */
    companion object {
        const val PAGE_SIZE = 20
    }

    /** 最近一次查询条件，供「加载更多」与「删除后刷新」复用，避免刷新到错误的成员。
     *  memberId 为 null 表示不按成员过滤（展示所有人的记录）。 */
    private data class RecordQuery(
        val memberId: Long?,
        val keyword: String?,
        val fromDate: Long?,
        val toDate: Long?
    )

    private var lastQuery: RecordQuery? = null

    private val _uiState = MutableStateFlow(MedicalRecordUiState())
    val uiState: StateFlow<MedicalRecordUiState> = _uiState.asStateFlow()

    /** 下拉刷新指示；refresh() 触发后由 loadRecords 在结束时复位。 */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

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

    /**
     * 加载记录。
     *
     * @param memberId 指定成员 id；为 null 时不按成员过滤，展示所有人的记录。
     * @param append true 时把本页数据追加到已有列表之后（分页加载），false 时替换列表。
     */
    fun loadRecords(
        memberId: Long? = null,
        keyword: String? = null,
        fromDate: Long? = null,
        toDate: Long? = null,
        offset: Int = 0,
        append: Boolean = false
    ) {
        lastQuery = RecordQuery(memberId, keyword, fromDate, toDate)
        _uiState.update {
            it.copy(
                isLoading = !append,
                isLoadingMore = append,
                error = null
            )
        }
        viewModelScope.launch {
            try {
                val kw = keyword?.trim()?.takeIf { it.isNotEmpty() }
                val likePattern = "%${kw ?: ""}%"

                val from = if (fromDate != null) {
                    java.time.Instant.ofEpochMilli(fromDate)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                } else {
                    LocalDateTime.of(1970, 1, 1, 0, 0)
                }

                val to = if (toDate != null) {
                    java.time.Instant.ofEpochMilli(toDate)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                        .withHour(23).withMinute(59).withSecond(59)
                } else {
                    LocalDateTime.of(9999, 12, 31, 23, 59, 59)
                }

                val page = if (memberId == null) {
                    recordRepository.searchAllPaged(
                        keyword = kw,
                        likePattern = likePattern,
                        from = from,
                        to = to,
                        limit = PAGE_SIZE,
                        offset = offset
                    )
                } else {
                    recordRepository.searchByMemberPaged(
                        patientId = memberId,
                        keyword = kw,
                        likePattern = likePattern,
                        from = from,
                        to = to,
                        limit = PAGE_SIZE,
                        offset = offset
                    )
                }

                _uiState.update { state ->
                    val merged = if (append) state.records + page else page
                    state.copy(
                        currentMemberId = memberId ?: 0L,
                        keyword = keyword,
                        fromDate = fromDate,
                        toDate = toDate,
                        records = merged,
                        hasMore = page.size >= PAGE_SIZE,
                        isLoading = false,
                        isLoadingMore = false
                    )
                }
                _isRefreshing.value = false
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message, isLoading = false, isLoadingMore = false)
                }
                _isRefreshing.value = false
            }
        }
    }

    /** 加载下一页；已在加载中或无更多数据时直接返回。 */
    fun loadMore() {
        val query = lastQuery ?: return
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        loadRecords(
            memberId = query.memberId,
            keyword = query.keyword,
            fromDate = query.fromDate,
            toDate = query.toDate,
            offset = state.records.size,
            append = true
        )
    }

    fun deleteRecord(record: MedicalRecord) {
        viewModelScope.launch {
            recordRepository.delete(record)
            // 用最近一次查询条件刷新，而不是 uiState.currentMemberId
            // （后者在选择成员的流程里可能尚未更新，会刷新到错误的成员）
            lastQuery?.let { query ->
                loadRecords(
                    memberId = query.memberId,
                    keyword = query.keyword,
                    fromDate = query.fromDate,
                    toDate = query.toDate
                )
            }
        }
    }

    fun setFilter(memberId: Long, keyword: String?, fromDate: Long?, toDate: Long?) {
        loadRecords(memberId, keyword, fromDate, toDate)
    }

    /** 手动下拉刷新：用最近一次查询条件重新加载第一页。 */
    fun refresh() {
        val query = lastQuery ?: return
        _isRefreshing.value = true
        loadRecords(query.memberId, query.keyword, query.fromDate, query.toDate)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    suspend fun getDefaultMember(): FamilyMember? = memberRepository.getDefaultMember()

    suspend fun insertMember(member: FamilyMember): Long = memberRepository.insert(member)

    suspend fun getLatestRecord(): MedicalRecord? = recordRepository.getLatestRecord()

    suspend fun getMemberById(id: Long): FamilyMember? = memberRepository.getMemberById(id)
}

data class MedicalRecordUiState(
    val members: List<FamilyMember> = emptyList(),
    val records: List<MedicalRecord> = emptyList(),
    val currentMemberId: Long = 0,
    val keyword: String? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    /** 是否正在加载下一页（与首次加载区分，用于底部指示器） */
    val isLoadingMore: Boolean = false,
    /** 是否还有下一页 */
    val hasMore: Boolean = true
)
