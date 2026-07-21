package com.yy.medtrace.data.repository

import org.junit.Test
import org.junit.Assert.*

class RepositorySimplificationTest {
    @Test
    fun `MemberRepository should have simplified interface`() {
        // 验证Repository接口已简化
        val interfaceMethods = MemberRepository::class.java.declaredMethods.map { it.name }
        
        // 验证只保留必要的方法
        assertTrue("getAllMembers should be present", interfaceMethods.contains("getAllMembers"))
        assertTrue("getDefaultMember should be present", interfaceMethods.contains("getDefaultMember"))
        assertTrue("getMemberById should be present", interfaceMethods.contains("getMemberById"))
        assertTrue("insert should be present", interfaceMethods.contains("insert"))
        assertTrue("update should be present", interfaceMethods.contains("update"))
        assertTrue("deleteById should be present", interfaceMethods.contains("deleteById"))
        
        // 验证移除的方法
        assertFalse("getAllMembersList should be removed", interfaceMethods.contains("getAllMembersList"))
        assertFalse("insertAll should be removed", interfaceMethods.contains("insertAll"))
        assertFalse("clear should be removed", interfaceMethods.contains("clear"))
    }
    
    @Test
    fun `RecordRepository should have simplified interface`() {
        // 验证Repository接口已简化
        val interfaceMethods = RecordRepository::class.java.declaredMethods.map { it.name }
        
        // 验证只保留必要的方法
        assertTrue("getAllRecords should be present", interfaceMethods.contains("getAllRecords"))
        assertTrue("getRecordsByMember should be present", interfaceMethods.contains("getRecordsByMember"))
        assertTrue("getUnknownRecords should be present", interfaceMethods.contains("getUnknownRecords"))
        assertTrue("insert should be present", interfaceMethods.contains("insert"))
        assertTrue("update should be present", interfaceMethods.contains("update"))
        assertTrue("delete should be present", interfaceMethods.contains("delete"))
        
        // 验证移除的方法
        assertFalse("getAllRecordsList should be removed", interfaceMethods.contains("getAllRecordsList"))
        assertFalse("getRecentRecordsByMember should be removed", interfaceMethods.contains("getRecentRecordsByMember"))
        assertFalse("countByMember should be removed", interfaceMethods.contains("countByMember"))
        assertFalse("insertAll should be removed", interfaceMethods.contains("insertAll"))
        assertFalse("clear should be removed", interfaceMethods.contains("clear"))
    }
}