package com.yy.chiyaole.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yy.chiyaole.data.llm.Metric
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.ui.theme.AppShapes
import kotlinx.serialization.json.Json

private fun parseMetrics(records: List<MedicalRecord>): List<Metric> {
    val result = mutableListOf<Metric>()
    for (r in records) {
        if (r.metricsJson.isBlank()) continue
        runCatching { Json.decodeFromString<List<Metric>>(r.metricsJson) }
            .getOrElse { emptyList() }
            .let { result.addAll(it) }
    }
    return result
}

@Composable
fun HealthTipsCard(
    member: FamilyMember? = null,
    recentRecords: List<MedicalRecord> = emptyList(),
    aiAdvice: String? = null
) {
    val tips = buildList {
        // 基于成员档案
        member?.let { m ->
            if (m.allergy.isNotBlank()) {
                add("过敏史：${m.allergy}。就医或用药前请主动告知医生，避免触发过敏。")
            }
            if (m.chronic.isNotBlank()) {
                add("慢性病管理：${m.chronic}。建议定期监测相关指标并遵医嘱规律用药。")
            }
            if (m.medicationNote.isNotBlank()) {
                add("用药注意：${m.medicationNote}。")
            }
            if (m.bloodType.isBlank()) {
                add("建议补全血型信息，便于紧急情况快速处置。")
            }
        }

        // 基于 AI 解析的检查指标
        val metrics = parseMetrics(recentRecords)
        val abnormal = metrics.filter { it.abnormal }
        if (abnormal.isNotEmpty()) {
            val names = abnormal.map { it.name }.distinct().joinToString("、")
            add("近期检查中 $names 超出参考范围，建议复查并咨询医生。")
        }
        if (metrics.isNotEmpty() && abnormal.isEmpty()) {
            add("近期 AI 识别的检查指标均在参考范围内，继续保持。")
        }

        // 通用提醒
        add("按时复诊，妥善保存历次就诊记录与检查报告。")
        add("规律作息、均衡膳食，有助于身体恢复。")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = "健康",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = if (member != null) "${member.name} 的健康建议" else "健康小贴士",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            aiAdvice?.takeIf { it.isNotBlank() }?.let { advice ->
                Text(
                    text = "AI 建议：\n$advice",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Text(
                text = tips.joinToString("\n") { "• $it" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
