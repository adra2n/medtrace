package com.yy.medtrace.viewmodel

import com.yy.medtrace.TestDispatcherRule
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.repository.MemberRepository
import com.yy.medtrace.data.repository.TodoRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class HomeViewModelTestWithRule {
    
    @get:Rule
    val testDispatcherRule = TestDispatcherRule()
    
    @MockK
    private lateinit var memberRepository: MemberRepository
    
    @MockK
    private lateinit var todoRepository: TodoRepository
    
    private lateinit var viewModel: HomeViewModel
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { memberRepository.getAllMembers() } returns flowOf(emptyList())
        every { todoRepository.getByDate(any()) } returns flowOf(emptyList())
        coEvery { memberRepository.insert(any()) } returns 1L
    }
    
    @Test
    fun `should create default member when list is empty`() = runTest {
        viewModel = HomeViewModel(memberRepository, todoRepository)
        
        // 验证插入默认成员被调用
        coVerify { memberRepository.insert(any()) }
    }
    
    @Test
    fun `should load members successfully`() = runTest {
        val members = listOf(FamilyMember(name = "测试用户", relation = "本人"))
        every { memberRepository.getAllMembers() } returns flowOf(members)
        
        viewModel = HomeViewModel(memberRepository, todoRepository)
        
        // 验证状态更新
        val state = viewModel.uiState.value
        assertEquals(members, state.members)
        assertNull(state.error)
    }
}