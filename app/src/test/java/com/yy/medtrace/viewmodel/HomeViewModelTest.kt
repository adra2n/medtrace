package com.yy.medtrace.viewmodel

import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.RecordRepository
import com.yy.medtrace.data.repository.TodoRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    
    @MockK
    private lateinit var memberRepository: MemberRepository
    
    @MockK
    private lateinit var todoRepository: TodoRepository
    
    @MockK
    private lateinit var recordRepository: RecordRepository
    
    private lateinit var viewModel: HomeViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        // 默认mock repositories
        every { memberRepository.getAllMembers() } returns flowOf(emptyList())
        every { todoRepository.getByDate(any()) } returns flowOf(emptyList())
        coEvery { memberRepository.insert(any()) } returns 1L
        every { recordRepository.getRecentRecords(any()) } returns flowOf(emptyList())
        coEvery { recordRepository.countByMembers(any()) } returns emptyList()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `should create default member when list is empty`() = runTest {
        viewModel = HomeViewModel(memberRepository, todoRepository, recordRepository)
        
        // 验证插入默认成员被调用
        coVerify { memberRepository.insert(any()) }
    }
}