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

    @Query("SELECT * FROM health_todos WHERE dueDate = :date AND done = 0 AND reminderTime = :time")
    suspend fun getPendingByDateAndTime(date: LocalDate, time: String): List<HealthTodo>

    @Query("SELECT DISTINCT reminderTime FROM health_todos WHERE dueDate = :date AND done = 0")
    suspend fun getPendingReminderTimesByDate(date: LocalDate): List<String>

    @Query("SELECT * FROM health_todos WHERE id = :id")
    suspend fun getById(id: Long): HealthTodo?

    @Insert
    suspend fun insert(todo: HealthTodo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<HealthTodo>)

    @Update
    suspend fun update(todo: HealthTodo)

    @Query("UPDATE health_todos SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Delete
    suspend fun delete(todo: HealthTodo)

    @Query("DELETE FROM health_todos")
    suspend fun clear()

    @Query("SELECT * FROM health_todos")
    suspend fun getAllList(): List<HealthTodo>

    @Query("SELECT COUNT(*) FROM health_todos")
    suspend fun count(): Int
}
