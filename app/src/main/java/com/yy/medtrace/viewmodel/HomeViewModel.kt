package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(
    private val memberRepository: MemberRepository,
    private val todoRepository: TodoRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadMembers()
        loadTodos()
    }
    
    private fun loadMembers() {
        viewModelScope.launch {
            memberRepository.getAllMembers()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { list ->
                    if (list.isEmpty()) {
                        // 创建默认成员
                        memberRepository.insert(
                            FamilyMember(name = "我自己", relation = "本人", isDefault = true)
                        )
                        return@collect
                    }
                    _uiState.update { it.copy(members = list) }
                }
        }
    }
    
    private fun loadTodos() {
        viewModelScope.launch {
            todoRepository.getByDate(LocalDate.now())
                .catch { }
                .collect { todos ->
                    _uiState.update { it.copy(todos = todos) }
                }
        }
    }
    
    fun addMember(member: FamilyMember) {
        viewModelScope.launch {
            memberRepository.insert(member)
        }
    }
    
    fun addTodo(memberId: Long, memberName: String, content: String, dueDate: LocalDate) {
        viewModelScope.launch {
            todoRepository.insert(
                HealthTodo(
                    memberId = memberId,
                    memberName = memberName,
                    content = content,
                    dueDate = dueDate
                )
            )
        }
    }
    
    fun toggleTodoDone(todoId: Long, done: Boolean) {
        viewModelScope.launch {
            todoRepository.setDone(todoId, done)
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
    val error: String? = null,
    val todayLabel: String = LocalDate.now().let { today ->
        val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[today.dayOfWeek.value % 7]
        today.format(DateTimeFormatter.ofPattern("M月d日")) + " · " + week
    }
)

class HomeViewModelFactory(
    private val memberRepository: MemberRepository,
    private val todoRepository: TodoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(memberRepository, todoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}