package com.yy.medtrace.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.dao.FamilyMemberDao
import com.yy.medtrace.data.model.FamilyMember
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class MemberRepositoryIntegrationTest {
    
    private lateinit var database: AppDatabase
    private lateinit var familyMemberDao: FamilyMemberDao
    private lateinit var repository: MemberRepository
    
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        familyMemberDao = database.familyMemberDao()
        repository = MemberRepositoryImpl(familyMemberDao)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `insert and retrieve member`() = runTest {
        val member = FamilyMember(name = "测试用户", relation = "本人")
        val id = repository.insert(member)
        
        val retrieved = repository.getMemberById(id)
        assertNotNull(retrieved)
        assertEquals("测试用户", retrieved?.name)
    }
    
    @Test
    fun `get all members should return flow`() = runTest {
        val member = FamilyMember(name = "测试用户", relation = "本人")
        repository.insert(member)
        
        val members = repository.getAllMembers().first()
        assertEquals(1, members.size)
        assertEquals("测试用户", members[0].name)
    }
    
    @Test
    fun `update member should work`() = runTest {
        val member = FamilyMember(name = "测试用户", relation = "本人")
        val id = repository.insert(member)
        
        val retrieved = repository.getMemberById(id)
        assertNotNull(retrieved)
        
        val updatedMember = retrieved!!.copy(name = "更新用户")
        repository.update(updatedMember)
        
        val updatedRetrieved = repository.getMemberById(id)
        assertNotNull(updatedRetrieved)
        assertEquals("更新用户", updatedRetrieved?.name)
    }
    
    @Test
    fun `delete member should work`() = runTest {
        val member = FamilyMember(name = "测试用户", relation = "本人")
        val id = repository.insert(member)
        
        repository.deleteById(id)
        
        val retrieved = repository.getMemberById(id)
        assertNull(retrieved)
    }
}