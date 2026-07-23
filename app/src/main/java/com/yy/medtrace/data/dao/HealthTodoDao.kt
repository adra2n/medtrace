package com.yy.medtrace.data.dao

import androidx.room.*
import com.yy.medtrace.data.model.HealthTodo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HealthTodoDao {
    @Query("SELECT * FROM health_todos ORDER BY done ASC, dueDate ASC, id DESC")
    fun getAll(): Flow<List<HealthTodo>>

    @Query("SELECT * FROM health_todos WHERE dueDate = :date ORDER BY done ASC, id DESC")
    fun getByDate(date: LocalDate): Flow<List<HealthTodo>>

    @Query("SELECT COUNT(*) FROM health_todos WHERE dueDate = :date AND done = 0")
    suspend fun getPendingCountByDate(date: LocalDate): Int

    @Query("SELECT * FROM health_todos WHERE dueDate = :date AND done = 0")
    suspend fun getPendingByDate(date: LocalDate): List<HealthTodo>

    @Query("SELECT * FROM health_todos WHERE id = :id")
    suspend fun getById(id: Long): HealthTodo?

    @Query("UPDATE health_todos SET notifiedDate = :date WHERE id IN (:ids)")
    suspend fun markNotified(ids: List<Long>, date: String)

    @Insert
    suspend fun insert(todo: HealthTodo): Long

    @Update
    suspend fun update(todo: HealthTodo)

    @Query("UPDATE health_todos SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Delete
    suspend fun delete(todo: HealthTodo)
}
