package com.yy.medtrace.data.llm

import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.settings.LlmSettingsStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException

data class ComprehensiveResult(
    val trend: String = "",
    val advice: String = "",
    val raw: String
)

class ComprehensiveAnalysisUseCase(private val settings: LlmSettingsStore) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun buildSummary(member: FamilyMember, records: List<MedicalRecord>): String {
        val profile = buildString {
            append("成员：${member.name}")
            if (member.relation.isNotBlank()) append("（${member.relation}）")
            if (member.gender.isNotBlank()) append("，性别：${member.gender}")
            if (member.birthday.isNotBlank()) append("，生日：${member.birthday}")
            if (member.bloodType.isNotBlank()) append("，血型：${member.bloodType}")
            if (member.allergy.isNotBlank()) append("；过敏史：${member.allergy}")
            if (member.chronic.isNotBlank()) append("；慢性病：${member.chronic}")
            if (member.medicationNote.isNotBlank()) append("；用药注意：${member.medicationNote}")
        }
        val recordsText = if (records.isEmpty()) {
            "（该成员暂无医疗记录）"
        } else {
            records.sortedByDescending { it.onsetTime }.joinToString("\n") { r ->
                buildString {
                    append("- 就诊时间：${r.onsetTime}；诊断：${r.diagnosis}")
                    if (r.hospital.isNotBlank()) append("；医院：${r.hospital}")
                    if (r.medItems.isNotEmpty()) {
                        append("；用药：" + r.medItems.joinToString("、") {
                            listOf(it.name, it.dose, it.freq).filter { v -> v.isNotBlank() }.joinToString(" ")
                        })
                    }
                    if (r.metricsJson.isNotBlank()) {
                        runCatching { json.decodeFromString<List<Metric>>(r.metricsJson) }.getOrNull()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { metrics ->
                                append("；检查指标：" + metrics.joinToString("、") {
                                    "${it.name}=${it.value}${it.unit}（参考${it.range}${if (it.abnormal) "，异常" else ""}）"
                                })
                            }
                    }
                    if (r.notes.isNotBlank()) append("；备注：${r.notes}")
                }
            }
        }
        return "$profile\n\n医疗记录（按时间倒序）：\n$recordsText"
    }

    private fun buildPrompt(summary: String): String = """
你是一名健康管理助手。以下是某位家庭成员的健康档案与历史就医记录，请基于这些信息进行分析。

$summary

请严格输出 JSON，包含两个字段：
- "trend"：对该成员健康指标与就诊情况的趋势解读（如指标变化、复诊频率、需注意的方向），用中文、简明有条理。
- "advice"：给该成员的个性化健康建议（结合过敏史、慢性病、用药与指标），用中文、可分点，语气温和、不作诊断、不替代医生。

仅输出 JSON，不要额外说明。
""".trimIndent()

    suspend fun analyze(member: FamilyMember, records: List<MedicalRecord>): ComprehensiveResult {
        val base = settings.getBaseUrl() ?: error("LLM base URL 未设置")
        val key = settings.getApiKey() ?: error("LLM API key 未设置")
        val model = settings.getModel() ?: "gpt-4o"
        val api = LlmApi.create(base.normalizeBaseUrl())
        val prompt = buildPrompt(buildSummary(member, records))
        val req = ChatRequest(model, listOf(Message("user", JsonPrimitive(prompt))))
        val raw = try {
            api.chat("Bearer $key", req).choices.first().message.content
        } catch (e: retrofit2.HttpException) {
            val body = e.response()?.errorBody()?.string() ?: e.message ?: "HTTP ${e.code()}"
            throw RuntimeException("HTTP ${e.code()}: $body")
        }
        return runCatching { parse(raw) }.getOrDefault(ComprehensiveResult(raw = raw))
    }

    private fun parse(raw: String): ComprehensiveResult {
        val obj = json.decodeFromString<RawResult>(extractJson(raw))
        return ComprehensiveResult(
            trend = obj.trend ?: "",
            advice = obj.advice ?: "",
            raw = raw
        )
    }

    @Serializable
    private data class RawResult(
        val trend: String? = null,
        val advice: String? = null
    )
}
