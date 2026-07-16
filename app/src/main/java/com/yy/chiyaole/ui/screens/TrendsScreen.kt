package com.yy.chiyaole.ui.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.llm.Metric
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.ui.components.MemberSelector
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class MetricPoint(
    val time: LocalDateTime,
    val value: Double,
    val abnormal: Boolean,
    val raw: String
)

private data class MetricSeries(
    val name: String,
    val unit: String,
    val range: String,
    val points: List<MetricPoint>
)

private fun parseNumeric(raw: String): Double? {
    val cleaned = raw.replace(Regex("[^0-9./]"), " ").trim()
    val first = cleaned.split(Regex("\\s+|/")).firstOrNull { it.isNotBlank() } ?: return null
    return first.toDoubleOrNull()
}

private fun buildSeries(records: List<MedicalRecord>): List<MetricSeries> {
    val byName = LinkedHashMap<String, MetricSeries>()
    for (r in records) {
        if (r.metricsJson.isBlank()) continue
        val metrics = runCatching {
            Json.decodeFromString<List<Metric>>(r.metricsJson)
        }.getOrElse { emptyList() }
        for (m in metrics) {
            val v = parseNumeric(m.value) ?: continue
            val series = byName.getOrPut(m.name) {
                MetricSeries(m.name, m.unit, m.range, emptyList())
            }
            byName[m.name] = series.copy(
                points = (series.points + MetricPoint(r.onsetTime, v, m.abnormal, m.value))
                    .sortedBy { it.time }
            )
        }
    }
    return byName.values.filter { it.points.size >= 1 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    database: AppDatabase,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedMemberId = SelectedMemberHolder.recordsSelectedMemberId.value
    val dayFmt = DateTimeFormatter.ofPattern("MM-dd")

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .catch { e -> error = e.message }
            .collect { list ->
                members = list
                if (SelectedMemberHolder.recordsSelectedMemberId.value == null && list.isNotEmpty()) {
                    SelectedMemberHolder.recordsSelectedMemberId.value = list.first().id
                }
            }
    }

    LaunchedEffect(selectedMemberId) {
        selectedMemberId?.let { id ->
            database.medicalRecordDao().getRecordsByMember(id)
                .catch { e -> error = e.message }
                .collectLatest { records = it }
        } ?: run { records = emptyList() }
    }

    val series = remember(records) { buildSeries(records) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    val current = series.firstOrNull { it.name == selectedName } ?: series.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("健康趋势") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (members.isNotEmpty()) {
                MemberSelector(
                    members = members,
                    selectedMemberId = selectedMemberId,
                    onSelect = { SelectedMemberHolder.recordsSelectedMemberId.value = it.id },
                    emptyHint = "暂无家庭成员，请先在家庭中添加"
                )
            }

            if (error != null) {
                Text("加载出错：$error", color = MaterialTheme.colorScheme.error)
            } else if (series.isEmpty()) {
                Text(
                    "该成员暂无 AI 解析的健康指标。\n在添加记录时使用「拍照识别 / 文本分析」，AI 提取的检查指标（如血压、血糖）会自动生成趋势图。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    series.forEach { s ->
                        FilterChip(
                            selected = s.name == current?.name,
                            onClick = { selectedName = s.name },
                            label = { Text(s.name) }
                        )
                    }
                }

                current?.let { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "${s.name}（${s.points.size} 次）",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (s.unit.isNotBlank() || s.range.isNotBlank()) {
                                Text(
                                    buildString {
                                        if (s.unit.isNotBlank()) append("单位：${s.unit}    ")
                                        if (s.range.isNotBlank()) append("参考范围：${s.range}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            if (s.points.size == 1) {
                                Text(
                                    "最新：${s.points.first().raw}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                TrendLineChart(s, dayFmt)
                            }
                            val abnormalCount = s.points.count { it.abnormal }
                            if (abnormalCount > 0) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "⚠ $abnormalCount 次指标超出参考范围",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendLineChart(series: MetricSeries, dayFmt: DateTimeFormatter) {
    val primary = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val outline = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()

    val height = 200.dp
    val padding = 32.dp

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val w = size.width
        val h = size.height
        val pad = padding.toPx()
        val pts = series.points
        val values = pts.map { it.value }
        val minV = (values.minOrNull() ?: 0.0) - 1.0
        val maxV = (values.maxOrNull() ?: 1.0) + 1.0
        val span = if (maxV - minV == 0.0) 1.0 else maxV - minV
        val innerW = w - pad * 2
        val innerH = h - pad * 2

        fun x(i: Int): Float = if (pts.size == 1) pad + innerW / 2f else pad + innerW * i / (pts.size - 1)
        fun y(v: Double): Float = (pad + innerH * (1f - ((v - minV) / span).toFloat()))

        // 网格基线
        drawLine(
            color = outline.copy(alpha = 0.3f),
            start = Offset(pad, h - pad),
            end = Offset(w - pad, h - pad),
            strokeWidth = 1.dp.toPx()
        )

        // 折线
        val path = Path()
        pts.forEachIndexed { i, p ->
            val px = x(i)
            val py = y(p.value)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, primary, style = Stroke(2.dp.toPx()))

        // 数据点
        pts.forEachIndexed { i, p ->
            val px = x(i)
            val py = y(p.value)
            drawCircle(
                color = if (p.abnormal) errorColor else primary,
                radius = 4.dp.toPx(),
                center = Offset(px, py)
            )
            val label = pts[i].time.format(dayFmt)
            val text = measurer.measure(label, androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = textColor))
            drawText(text, topLeft = Offset(px - text.size.width / 2f, h - pad + 6.dp.toPx()))
            val valText = measurer.measure(p.raw, androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = textColor))
            drawText(valText, topLeft = Offset(px - valText.size.width / 2f, py - valText.size.height - 6.dp.toPx()))
        }
    }
}
