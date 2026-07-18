package com.yy.medtrace.data.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class AnalysisUseCaseTest {

    @Test
    fun extractJson_stripsFencedBlock() {
        val raw = "```json\n{\"diagnosis\":\"感冒\",\"date\":\"2026-07-15\"}\n```"
        val json = extractJson(raw)
        assertEquals("{\"diagnosis\":\"感冒\",\"date\":\"2026-07-15\"}", json)
    }

    @Test
    fun extractJson_stripsProseAroundJson() {
        val raw = "好的，这是提取结果：\n{\"diagnosis\":\"发烧\"}\n以上为结果。"
        val json = extractJson(raw)
        assertEquals("{\"diagnosis\":\"发烧\"}", json)
    }

    @Test
    fun preferredVisitDateTime_parsesIsoDate() {
        val r = AnalysisResult(date = "2026-07-15", raw = "{}")
        assertEquals(LocalDateTime.of(2026, 7, 15, 0, 0), r.preferredVisitDateTime())
    }

    @Test
    fun preferredVisitDateTime_returnsNullWhenBlank() {
        assertNull(AnalysisResult(date = "", raw = "{}").preferredVisitDateTime())
        assertNull(AnalysisResult(date = null, raw = "{}").preferredVisitDateTime())
    }
}
