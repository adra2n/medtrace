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
    
    init {
        loadMembers()
        loadTodos()
        loadRecentRecords()
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
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(error = result.message) }
                        }
                        is Result.Loading -> {
                            // 不需要处理加载状态
                        }
                    }
                }
        }
    }
    
    private fun loadTodos() {
        viewModelScope.launch {
            todoRepository.getByDate(LocalDate.now())
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update { it.copy(todos = result.data, pendingCount = result.data.count { !it.done }) }
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(error = result.message) }
                        }
                        is Result.Loading -> {
                            // 不需要处理加载状态
                        }
                    }
                }
        }
    }
    
    private fun loadRecentRecords() {
        viewModelScope.launch {
            recordRepository.getRecentRecords(3)
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update { it.copy(recentRecords = result.data) }
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(error = result.message) }
                        }
                        is Result.Loading -> {
                            // 不需要处理加载状态
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
        repeatInterval: Int = 1,
        category: String = "其他"
    ) {
        viewModelScope.launch {
            todoRepository.insert(
                HealthTodo(
                    memberId = memberId,
                    memberName = memberName,
                    content = content,
                    dueDate = dueDate,
                    repeatType = repeatType,
                    repeatInterval = repeatInterval,
                    category = category
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
}

data class HomeUiState(
    val members: List<FamilyMember> = emptyList(),
    val todos: List<HealthTodo> = emptyList(),
    val recentRecords: List<MedicalRecord> = emptyList(),
    val pendingCount: Int = 0,
    val error: String? = null,
    val todayLabel: String = LocalDate.now().let { today ->
        val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[today.dayOfWeek.value % 7]
        today.format(DateTimeFormatter.ofPattern("M月d日")) + " · " + week
    }
)
