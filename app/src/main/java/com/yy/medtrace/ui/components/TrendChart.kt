package com.yy.medtrace.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.Success
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yy.medtrace.data.llm.Metric
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MetricPoint(
    val time: LocalDateTime,
    val value: Double,
    val abnormal: Boolean,
    val raw: String
)

data class MetricSeries(
    val name: String,
    val unit: String,
    val range: String,
    val points: List<MetricPoint>
)

fun parseNumeric(raw: String): Double? {
    val cleaned = raw.replace(Regex("[^0-9./]"), " ").trim()
    val first = cleaned.split(Regex("\\s+|/")).firstOrNull { it.isNotBlank() } ?: return null
    return first.toDoubleOrNull()
}

// 解析参考范围，如 "90-140" / "90~140" / "90—140" -> (low, high)
fun parseRange(range: String): Pair<Double, Double>? {
    if (range.isBlank()) return null
    val parts = range.split(Regex("[-~—~]")).mapNotNull { parseNumeric(it) }
    if (parts.size != 2) return null
    val low = parts[0]
    val high = parts[1]
    if (low >= high) return null
    return low to high
}

// 血压类指标（如 "120/80"）拆成收缩压 / 舒张压两条独立序列，各自带参考范围。
private fun expandMetric(m: Metric): List<Metric> {
    val rawParts = m.value.split("/").map { it.trim() }
    val numericParts = rawParts.mapNotNull { parseNumeric(it) }
    val isBp = numericParts.size == 2 ||
        m.name.contains("压") && m.value.contains("/")
    if (!isBp || numericParts.size != 2) return listOf(m)
    val ranges = m.range.split("/").map { it.trim() }
    val (sysRange, diaRange) = if (ranges.size == 2) ranges[0] to ranges[1] else "" to ""
    return listOf(
        Metric("${m.name}·收缩压", rawParts[0], m.unit, sysRange, m.abnormal),
        Metric("${m.name}·舒张压", rawParts[1], m.unit, diaRange, m.abnormal)
    )
}

fun buildSeries(records: List<MedicalRecord>): List<MetricSeries> {
    return emptyList()
}

@Composable
fun TrendSection(
    series: List<MetricSeries>,
    modifier: Modifier = Modifier
) {
    val dayFmt = DateTimeFormatter.ofPattern("MM-dd")
    var selectedName by remember { mutableStateOf<String?>(null) }
    val current = series.firstOrNull { it.name == selectedName } ?: series.firstOrNull()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (series.isEmpty()) {
            Text(
                stringResource(R.string.trend_chart_empty_hint),
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
                    shape = AppShapes.large,
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

@Composable
fun TrendLineChart(series: MetricSeries, dayFmt: DateTimeFormatter) {
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

        // 正常参考范围绿带
        val range = parseRange(series.range)
        if (range != null) {
            val yTop = y(range.second).coerceIn(pad, h - pad)
            val yBottom = y(range.first).coerceIn(pad, h - pad)
            drawRect(
                color = Success.copy(alpha = 0.16f),
                topLeft = Offset(pad, yTop),
                size = androidx.compose.ui.geometry.Size(innerW, (yBottom - yTop).coerceAtLeast(0f))
            )
        }

        drawLine(
            color = outline.copy(alpha = 0.25f),
            start = Offset(pad, h - pad),
            end = Offset(w - pad, h - pad),
            strokeWidth = 1.dp.toPx()
        )

        val path = Path()
        pts.forEachIndexed { i, p ->
            val px = x(i)
            val py = y(p.value)
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }

        // 线下渐变填充（面积图）
        val fillPath = Path().apply {
            addPath(path)
            lineTo(x(pts.size - 1), h - pad)
            lineTo(pad, h - pad)
            close()
        }
        drawPath(
            fillPath,
            Brush.verticalGradient(
                colors = listOf(primary.copy(alpha = 0.28f), primary.copy(alpha = 0.02f)),
                startY = pad,
                endY = h - pad
            )
        )

        // 折线上方细高光线
        drawPath(path, primary.copy(alpha = 0.15f), style = Stroke(5.dp.toPx()))

        drawPath(path, primary, style = Stroke(2.5.dp.toPx()))

        pts.forEachIndexed { i, p ->
            val px = x(i)
            val py = y(p.value)
            drawCircle(
                color = if (p.abnormal) errorColor else primary,
                radius = 4.5.dp.toPx(),
                center = Offset(px, py)
            )
            if (p.abnormal) {
                drawCircle(
                    color = errorColor.copy(alpha = 0.2f),
                    radius = 9.dp.toPx(),
                    center = Offset(px, py)
                )
            }
            val label = pts[i].time.format(dayFmt)
            val text = measurer.measure(label, androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = textColor))
            drawText(text, topLeft = Offset(px - text.size.width / 2f, h - pad + 6.dp.toPx()))
            val valText = measurer.measure(p.raw, androidx.compose.ui.text.TextStyle(fontSize = 9.sp, color = textColor))
            drawText(valText, topLeft = Offset(px - valText.size.width / 2f, py - valText.size.height - 6.dp.toPx()))
        }
    }
}
