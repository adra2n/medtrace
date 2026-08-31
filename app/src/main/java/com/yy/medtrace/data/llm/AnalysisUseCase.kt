package com.yy.medtrace.data.llm

import com.yy.medtrace.data.settings.LlmSettingsStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AnalysisResult(
    val diagnosis: String? = null,
    val metrics: List<Metric> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val allergies: List<String> = emptyList(),
    val items: List<Item> = emptyList(),
    val hospital: String? = null,
    val source: String? = null,
    val date: String? = null,
    val followUp: String? = null,
    val raw: String
)

@Serializable
data class Medication(val name: String, val dosage: String, val frequency: String, val usage: String)
@Serializable
data class Metric(val name: String, val value: String, val unit: String, val range: String, val abnormal: Boolean)
@Serializable
data class Item(val name: String, val dose: String, val note: String)

class AnalysisUseCase(settings: LlmSettingsStore) : BaseLlmUseCase(settings) {

    suspend fun analyze(text: String, images: List<String>): AnalysisResult {
        val model = getModel()
        val content: JsonArray = buildJsonArray {
            images.forEach { url ->
                add(buildJsonObject {
                    put("type", "image_url")
                    putJsonObject("image_url") { put("url", url) }
                })
            }
            add(buildJsonObject {
                put("type", "text")
                put("text", buildPrompt(text, images.isNotEmpty()))
            })
        }
        val req = ChatRequest(model, listOf(Message("user", content)))
        val raw = callApi(req)
        return runCatching { parse(raw) }.getOrDefault(AnalysisResult(raw = raw))
    }

    private fun parse(raw: String): AnalysisResult {
        val obj = json.decodeFromString<RawAnalysis>(extractJson(raw))
        return AnalysisResult(obj.diagnosis, obj.metrics ?: emptyList(),
            obj.medications ?: emptyList(), obj.allergies ?: emptyList(), obj.items ?: emptyList(),
            obj.hospital, obj.source, obj.date, obj.followUp, raw)
    }

    private fun buildPrompt(userText: String, hasImage: Boolean): String = """
你是一名医疗与健康信息整理助手。用户会随手拍下处方、药品或医疗单据（图片或文字），请提取重要信息并整理为结构化 JSON。
字段说明：
- diagnosis: 诊断 / 主诉（无则空）。
- medications: 用药数组 [{name, dosage, frequency, usage}]，例如 {name:"阿莫西林", dosage:"0.5g", frequency:"每日3次", usage:"饭后服用"}。
- metrics: 检查指标数组 [{name, value, unit, range, abnormal}]（无则空数组）。
- allergies: 过敏史字符串数组（无则空数组）。
- items: 物品 / 项目数组 [{name, dose, note}]（如药品、器械，无则空数组）。
- hospital: 就诊医院名称（无则空）。
- source: 来源（医院 / 药店 / 单据方，无则空）。
- date: 就诊或单据日期，格式 YYYY-MM-DD（无则空）。
- followUp: 随访 / 注意事项（无则空）。
仅整理信息，不作诊断，不给出医疗建议。输出严格 JSON。
原始内容: $userText
""".trimIndent()

    @Serializable
    private data class RawAnalysis(
        val diagnosis: String? = null,
        val metrics: List<Metric> = emptyList(),
        val medications: List<Medication> = emptyList(),
        val allergies: List<String> = emptyList(),
        val items: List<Item> = emptyList(),
        val hospital: String? = null,
        val source: String? = null,
        val date: String? = null,
        val followUp: String? = null
    )
}

fun AnalysisResult.preferredVisitDateTime(): LocalDateTime? {
    val d = date ?: return null
    if (d.isBlank()) return null
    return runCatching {
        LocalDate.parse(d.trim(), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay()
    }.getOrNull()
}

internal fun extractJson(raw: String): String {
    var s = raw.trim()
    s = s.replace(Regex("^```[a-zA-Z]*\\s*"), "").replace(Regex("\\s*```$"), "").trim()
    val start = s.indexOf('{')
    val end = s.lastIndexOf('}')
    if (start >= 0 && end > start) s = s.substring(start, end + 1)
    return s
}

internal fun String.normalizeBaseUrl(): String {
    var u = this.trim().removeSuffix("/").removeSuffix("/chat/completions")
    if (!u.endsWith("/")) u = "$u/"
    return u
}
