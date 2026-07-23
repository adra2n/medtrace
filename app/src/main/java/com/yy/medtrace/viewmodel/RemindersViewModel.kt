package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class RemindersViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    fun loadReminders() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val membersFlow = database.familyMemberDao().getAllMembers()
            val todosFlow = database.healthTodoDao().getAll()
            combine(membersFlow, todosFlow) { m, t -> m to t }
                .collect { (m, t) ->
                    _uiState.update { it.copy(
                        members = m,
                        todos = t.sortedWith(compareBy({ it.done }, { it.dueDate })),
                        isLoading = false
                    ) }
                }
        }
    }

    fun setTodoDone(todoId: Long, done: Boolean) {
        viewModelScope.launch {
            database.healthTodoDao().setDone(todoId, done)
        }
    }

    fun deleteTodo(todo: HealthTodo) {
        viewModelScope.launch {
            database.healthTodoDao().delete(todo)
        }
    }

    fun insertTodo(todo: HealthTodo) {
        viewModelScope.launch {
            database.healthTodoDao().insert(todo)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class RemindersUiState(
    val members: List<FamilyMember> = emptyList(),
    val todos: List<HealthTodo> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false
)

class RemindersViewModelFactory(
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RemindersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RemindersViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}