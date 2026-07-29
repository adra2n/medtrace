package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
@RequiresApi(Build.VERSION_CODES.O)
class RemindersViewModel @Inject constructor(
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
                    val sorted = t.sortedWith(compareBy({ it.done }, { it.dueDate }))
                    _uiState.update { it.copy(
                        members = m,
                        todos = sorted,
                        monthlyStats = calculateMonthlyStats(sorted),
                        isLoading = false
                    ) }
                }
        }
    }

    fun setTodoDone(todoId: Long, done: Boolean) {
        viewModelScope.launch {
            val todo = database.healthTodoDao().getById(todoId) ?: return@launch
            val today = LocalDate.now().toString()
            val newCompletedDates = if (done) {
                if (todo.completedDates.isBlank()) today
                else "${todo.completedDates},$today"
            } else {
                todo.completedDates.split(",").filter { it.trim() != today }.joinToString(",")
            }
            database.healthTodoDao().update(todo.copy(
                done = done,
                completedDates = newCompletedDates
            ))
            if (done && todo.repeatType != "none") {
                val nextDate = calculateNextDueDate(todo.dueDate, todo.repeatType, todo.repeatInterval)
                database.healthTodoDao().insert(
                    todo.copy(
                        id = 0,
                        dueDate = nextDate,
                        done = false,
                        notifiedDate = "",
                        completedDates = ""
                    )
                )
            }
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

    fun updateRepeat(todoId: Long, repeatType: String, repeatInterval: Int) {
        viewModelScope.launch {
            val todo = database.healthTodoDao().getById(todoId) ?: return@launch
            database.healthTodoDao().update(
                todo.copy(repeatType = repeatType, repeatInterval = repeatInterval)
            )
        }
    }

    fun updateTodo(todo: HealthTodo) {
        viewModelScope.launch {
            database.healthTodoDao().update(todo)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun calculateMonthlyStats(todos: List<HealthTodo>): MonthlyStats {
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())

        val monthTodos = todos.filter { it.dueDate in monthStart..monthEnd }
        val completed = monthTodos.count { it.done }
        val overdue = monthTodos.count { !it.done && it.dueDate.isBefore(today) }

        val completedDays = monthTodos.filter { it.done }.map { it.dueDate }.toSet()
        val overdueDays = monthTodos.filter { !it.done && it.dueDate.isBefore(today) }.map { it.dueDate }.toSet()

        val streak = calculateStreak(todos)

        return MonthlyStats(
            total = monthTodos.size,
            completed = completed,
            overdue = overdue,
            completionRate = if (monthTodos.isNotEmpty()) completed.toFloat() / monthTodos.size else 0f,
            streak = streak,
            completedDays = completedDays,
            overdueDays = overdueDays
        )
    }

    private fun calculateStreak(todos: List<HealthTodo>): Int {
        val today = LocalDate.now()
        var streak = 0
        var checkDate = today

        while (true) {
            val dayTodos = todos.filter { it.dueDate == checkDate }
            if (dayTodos.isEmpty()) break
            if (dayTodos.all { it.done }) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    companion object {
        fun calculateNextDueDate(current: LocalDate, type: String, interval: Int): LocalDate = when (type) {
            "day" -> current.plusDays(interval.toLong())
            "week" -> current.plusWeeks(interval.toLong())
            "month" -> current.plusMonths(interval.toLong())
            "year" -> current.plusYears(interval.toLong())
            else -> current
        }

        fun repeatLabel(type: String, interval: Int): String? = when (type) {
            "none" -> null
            "day" -> if (interval == 1) "每天" else "每 $interval 天"
            "week" -> if (interval == 1) "每周" else "每 $interval 周"
            "month" -> if (interval == 1) "每月" else "每 $interval 月"
            "year" -> if (interval == 1) "每年" else "每 $interval 年"
            else -> null
        }
    }
}

data class MonthlyStats(
    val total: Int = 0,
    val completed: Int = 0,
    val overdue: Int = 0,
    val completionRate: Float = 0f,
    val streak: Int = 0,
    val completedDays: Set<LocalDate> = emptySet(),
    val overdueDays: Set<LocalDate> = emptySet()
)

data class RemindersUiState(
    val members: List<FamilyMember> = emptyList(),
    val todos: List<HealthTodo> = emptyList(),
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val error: String? = null,
    val isLoading: Boolean = false
)
