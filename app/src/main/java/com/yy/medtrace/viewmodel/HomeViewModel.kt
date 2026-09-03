package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.common.Result
import com.yy.medtrace.common.asResultWithoutLoading
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.RecordRepository
import com.yy.medtrace.data.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val todoRepository: TodoRepository,
    private val recordRepository: RecordRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** 下拉刷新指示；refresh() 拉取快照后复位，不新增常驻 collector。 */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** 首页首屏依赖三路数据，全部到位后才解除加载态（避免空白一闪）。 */
    private val loadedSources = java.util.concurrent.atomic.AtomicInteger(0)

    private fun markSourceLoaded() {
        if (loadedSources.incrementAndGet() >= TOTAL_SOURCES) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    init {
        loadMembers()
        loadTodos()
        loadRecentRecords()
    }

    companion object {
        private const val TOTAL_SOURCES = 3
    }

    private fun loadMembers() {
        viewModelScope.launch {
            memberRepository.getAllMembers()
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            if (result.data.isEmpty()) {
                                memberRepository.insert(
                                    FamilyMember.DEFAULT
                                )
                            } else {
                                _uiState.update { it.copy(members = result.data) }
                            }
                            markSourceLoaded()
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(error = result.message) }
                            markSourceLoaded()
                        }
                        is Result.Loading -> {
                            // asResultWithoutLoading 不产生 Loading，仅保留分支完整性
                        }
                    }
                }
        }
    }

    /**
     * 首页「最近提醒」：不再按"今天"过滤，改为全量提醒。
     * DAO 已按 done ASC, dueDate ASC 排序，天然是「未完成的最近提醒优先（含逾期）」，
     * 页面只取前若干条展示。
     */
    private fun loadTodos() {
        viewModelScope.launch {
            todoRepository.getAll()
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update { it.copy(todos = result.data, pendingCount = result.data.count { !it.done }) }
                            markSourceLoaded()
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(error = result.message) }
                            markSourceLoaded()
                        }
                        is Result.Loading -> {
                            // asResultWithoutLoading 不产生 Loading，仅保留分支完整性
                        }
                    }
                }
        }
    }
    
    /** 首页「最新记录」：每位成员各取最近一次就诊记录，按时间倒序平铺。 */
    private fun loadRecentRecords() {
        viewModelScope.launch {
            recordRepository.getLatestRecordPerMember()
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update { it.copy(recentRecords = result.data) }
                            markSourceLoaded()
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(error = result.message) }
                            markSourceLoaded()
                        }
                        is Result.Loading -> {
                            // asResultWithoutLoading 不产生 Loading，仅保留分支完整性
                        }
                    }
                }
        }
    }
    
    fun addMember(member: FamilyMember) {
        viewModelScope.launch {
            memberRepository.insert(member)
        }
    }
    
    fun addTodo(
        memberId: Long,
        memberName: String,
        content: String,
        dueDate: LocalDate,
        repeatType: String = "none",
        category: String = "其他",
        reminderTime: String = "09:00"
    ) {
        viewModelScope.launch {
            todoRepository.insert(
                HealthTodo(
                    memberId = memberId,
                    memberName = memberName,
                    content = content,
                    dueDate = dueDate,
                    repeatType = repeatType,
                    category = category,
                    reminderTime = reminderTime
                )
            )
        }
    }
    
    fun toggleTodoDone(todoId: Long, done: Boolean) {
        viewModelScope.launch {
            todoRepository.toggleTodoDone(todoId, done)
        }
    }
    
    fun deleteTodo(todo: HealthTodo) {
        viewModelScope.launch {
            todoRepository.delete(todo)
        }
    }
    
    fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** 手动下拉刷新：重新拉取三路数据快照（避免重复常驻 collector 造成泄漏）。 */
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val members = memberRepository.getAllMembers().first().also {
                    if (it.isEmpty()) memberRepository.insert(FamilyMember.DEFAULT)
                }
                val todos = todoRepository.getAll().first()
                val records = recordRepository.getLatestRecordPerMember().first()
                _uiState.update {
                    it.copy(
                        members = members,
                        todos = todos,
                        recentRecords = records,
                        pendingCount = todos.count { !it.done }
                    )
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}

data class HomeUiState(
    val members: List<FamilyMember> = emptyList(),
    val todos: List<HealthTodo> = emptyList(),
    val recentRecords: List<MedicalRecord> = emptyList(),
    val pendingCount: Int = 0,
    val error: String? = null,
    /** 首屏三路数据是否仍在加载中 */
    val isLoading: Boolean = true,
    val todayLabel: String = LocalDate.now().let { today ->
        val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[today.dayOfWeek.value % 7]
        today.format(DateTimeFormatter.ofPattern("M月d日")) + " · " + week
    }
)
