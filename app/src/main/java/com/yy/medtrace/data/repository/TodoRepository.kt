package com.yy.medtrace.data.repository

import com.yy.medtrace.data.model.HealthTodo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TodoRepository {
    fun getAll(): Flow<List<HealthTodo>>
    fun getByDate(date: LocalDate): Flow<List<HealthTodo>>
    suspend fun getPendingCountByDate(date: LocalDate): Int
    suspend fun getPendingByDate(date: LocalDate): List<HealthTodo>
    suspend fun insert(todo: HealthTodo): Long
    suspend fun update(todo: HealthTodo)
    suspend fun setDone(id: Long, done: Boolean)
    suspend fun delete(todo: HealthTodo)
    suspend fun toggleTodoDone(id: Long, done: Boolean)
    suspend fun getById(id: Long): HealthTodo?
}