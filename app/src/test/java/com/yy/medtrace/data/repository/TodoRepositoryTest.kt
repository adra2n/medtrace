package com.yy.medtrace.data.repository

import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.dao.HealthTodoDao
import com.yy.medtrace.data.model.HealthTodo
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class TodoRepositoryTest {
    
    @MockK
    private lateinit var database: AppDatabase
    
    @MockK
    private lateinit var healthTodoDao: HealthTodoDao
    
    private lateinit var repository: TodoRepository
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = TodoRepositoryImpl(database, healthTodoDao)
    }
    
    @Test
    fun `getByDate should return todos from dao`() = runTest {
        val date = LocalDate.now()
        val todos = listOf(HealthTodo(content = "测试待办", dueDate = date))
        every { healthTodoDao.getByDate(date) } returns flowOf(todos)
        
        repository.getByDate(date).collect { 
            assertEquals(todos, it)
        }
    }
    
    @Test
    fun `insert should call dao insert`() = runTest {
        val todo = HealthTodo(content = "测试待办", dueDate = LocalDate.now())
        coEvery { healthTodoDao.insert(todo) } returns 1L
        
        val result = repository.insert(todo)
        
        assertEquals(1L, result)
        coVerify { healthTodoDao.insert(todo) }
    }
}