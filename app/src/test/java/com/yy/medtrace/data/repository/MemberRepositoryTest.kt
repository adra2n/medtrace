package com.yy.medtrace.data.repository

import com.yy.medtrace.data.dao.FamilyMemberDao
import com.yy.medtrace.data.model.FamilyMember
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class MemberRepositoryTest {
    
    @MockK
    private lateinit var familyMemberDao: FamilyMemberDao
    
    private lateinit var repository: MemberRepository
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = MemberRepositoryImpl(familyMemberDao)
    }
    
    @Test
    fun `getAllMembers should return members from dao`() = runTest {
        val members = listOf(FamilyMember(name = "测试用户", relation = "本人"))
        every { familyMemberDao.getAllMembers() } returns flowOf(members)
        
        repository.getAllMembers().collect { 
            assertEquals(members, it)
        }
    }
    
    @Test
    fun `insert should call dao insert`() = runTest {
        val member = FamilyMember(name = "测试用户", relation = "本人")
        coEvery { familyMemberDao.insert(member) } returns 1L
        
        val result = repository.insert(member)
        
        assertEquals(1L, result)
        coVerify { familyMemberDao.insert(member) }
    }
}