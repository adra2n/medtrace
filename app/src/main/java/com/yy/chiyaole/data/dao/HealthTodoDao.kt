package com.yy.chiyaole.data.dao

import androidx.room.*
import com.yy.chiyaole.data.model.HealthTodo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HealthTodoDao {
    @Query("SELECT * FROM health_todos WHERE dueDate = :date ORDER BY done ASC, id DESC")
    fun getByDate(date: LocalDate): Flow<List<HealthTodo>>

    @Insert
    suspend fun insert(todo: HealthTodo): Long

    @Update
    suspend fun update(todo: HealthTodo)

    @Query("UPDATE health_todos SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Delete
    suspend fun delete(todo: HealthTodo)
}
