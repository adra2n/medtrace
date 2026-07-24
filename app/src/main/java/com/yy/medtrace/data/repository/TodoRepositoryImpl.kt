package com.yy.medtrace.data.repository

import com.yy.medtrace.data.dao.HealthTodoDao
import com.yy.medtrace.data.model.HealthTodo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TodoRepositoryImpl(private val healthTodoDao: HealthTodoDao) : TodoRepository {
    
    override fun getByDate(date: LocalDate): Flow<List<HealthTodo>> {
        return healthTodoDao.getByDate(date)
    }
    
    override suspend fun getPendingCountByDate(date: LocalDate): Int {
        return healthTodoDao.getPendingCountByDate(date)
    }
    
    override suspend fun getPendingByDate(date: LocalDate): List<HealthTodo> {
        return healthTodoDao.getPendingByDate(date)
    }
    
    override suspend fun markNotified(ids: List<Long>, date: String) {
        healthTodoDao.markNotified(ids, date)
    }
    
    override suspend fun insert(todo: HealthTodo): Long {
        return healthTodoDao.insert(todo)
    }
    
    override suspend fun update(todo: HealthTodo) {
        healthTodoDao.update(todo)
    }
    
    override suspend fun setDone(id: Long, done: Boolean) {
        healthTodoDao.setDone(id, done)
    }
    
    override suspend fun delete(todo: HealthTodo) {
        healthTodoDao.delete(todo)
    }
    
    override suspend fun toggleTodoDone(id: Long, done: Boolean) {
        val todo = healthTodoDao.getById(id) ?: return
        val today = LocalDate.now().toString()
        val newCompletedDates = if (done) {
            if (todo.completedDates.isBlank()) today
            else "${todo.completedDates},$today"
        } else {
            todo.completedDates.split(",").filter { it.trim() != today }.joinToString(",")
        }
        healthTodoDao.updateCompletedDates(id, newCompletedDates)
        healthTodoDao.setDone(id, done)
    }
    
    override suspend fun getById(id: Long): HealthTodo? {
        return healthTodoDao.getById(id)
    }
}