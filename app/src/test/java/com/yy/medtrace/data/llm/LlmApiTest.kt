package com.yy.medtrace.data.llm

import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class LlmApiTest {
    @Test
    fun `create with same baseUrl returns same instance`() {
        val a = LlmApi.create("https://api.example.com/v1")
        val b = LlmApi.create("https://api.example.com/v1")
        assertSame(a, b)
    }

    @Test
    fun `create with different baseUrl returns different instance`() {
        val a = LlmApi.create("https://api.example.com/v1")
        val b = LlmApi.create("https://other.example.com/v1")
        assertNotSame(a, b)
    }

    @Test
    fun `create with normalized equivalent baseUrl returns same instance`() {
        val a = LlmApi.create("http://a")
        val b = LlmApi.create("http://a/")
        assertSame(a, b)
    }
}
