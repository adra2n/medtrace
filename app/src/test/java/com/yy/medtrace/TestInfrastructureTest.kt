package com.yy.medtrace

import org.junit.Test
import org.junit.Assert.*

class TestInfrastructureTest {
    @Test
    fun `TestDispatcherRule should provide test dispatcher`() {
        // 验证测试基础设施
        val rule = TestDispatcherRule()
        assertNotNull(rule.testDispatcher)
    }
    
    @Test
    fun `MainCoroutineRule should provide test dispatcher`() {
        // 验证测试基础设施
        val rule = MainCoroutineRule()
        assertNotNull(rule.testDispatcher)
    }
}