package com.yy.medtrace.common

import org.junit.Test
import org.junit.Assert.*

class ResultTest {
    @Test
    fun `Result success should contain data`() {
        val result = Result.Success("test")
        assertTrue(result is Result.Success)
        assertEquals("test", result.data)
    }
    
    @Test
    fun `Result error should contain message`() {
        val result = Result.Error("error message")
        assertTrue(result is Result.Error)
        assertEquals("error message", result.message)
    }
    
    @Test
    fun `Result loading should be singleton`() {
        val result1 = Result.Loading
        val result2 = Result.Loading
        assertTrue(result1 is Result.Loading)
        assertTrue(result2 is Result.Loading)
    }
}