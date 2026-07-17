package com.yy.chiyaole.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.llm.ComprehensiveAnalysisUseCase
import com.yy.chiyaole.data.llm.Metric
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.model.UserSettings
import com.yy.chiyaole.data.settings.LlmSettingsStore
import com.yy.chiyaole.ui.components.EmptyRecords
import com.yy.chiyaole.ui.components.MedicalRecordCard
import com.yy.chiyaole.ui.components.TrendSection
import com.yy.chiyaole.ui.components.buildSeries
import com.yy.chiyaole.ui.components.MemberSelector
import com.yy.chiyaole.ui.components.SectionCard
import com.yy.chiyaole.ui.components.parseNumeric
import com.yy.chiyaole.ui.components.InfoRow
import com.yy.chiyaole.ui.theme.AppShapes
import com.yy.chiyaole.ui.theme.GradientTopBar
import com.yy.chiyaole.ui.theme.PrimaryGradient
import com.yy.chiyaole.ui.theme.SoftElevation
import com.yy.chiyaole.ui.theme.cardContainerColor
import com.yy.chiyaole.Screen
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    database: AppDatabase,
    navController: NavController
) {
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var recentRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var trendRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var aiTrend by remember { mutableStateOf<String?>(null) }
    var aiAnalyzing by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedMemberId = SelectedMemberHolder.homeSelectedMemberId.value

    fun runAiAnalysis() {
        val member = members.firstOrNull { it.id == selectedMemberId } ?: return
        aiError = null
        aiAnalyzing = true
        scope.launch {
            try {
                val result = ComprehensiveAnalysisUseCase(LlmSettingsStore(context))
                    .analyze(member, trendRecords)
                aiTrend = result.trend.ifBlank { result.raw }
            } catch (e: Exception) {
                aiError = e.message ?: "分析失败"
            } finally {
                aiAnalyzing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .catch { e -> error = e.message }
            .collect { list ->
                if (list.isEmpty()) {
                    scope.launch {
                        database.familyMemberDao().insert(
                            FamilyMember(name = "我自己", relation = "本人", isDefault = true)
                        )
                    }
                    return@collect
                }
                members = list
                val persisted = database.userSettingsDao().getUserSettings().firstOrNull()?.selectedMemberId
                val validPersisted = if (persisted != null && persisted != 0L && list.any { it.id == persisted }) persisted else null
                if (SelectedMemberHolder.homeSelectedMemberId.value == null) {
                    SelectedMemberHolder.homeSelectedMemberId.value =
                        validPersisted ?: (database.medicalRecordDao().getLatestRecord()?.patientId ?: list.first().id)
                }
            }
    }

    val greeting = remember {
        val hour = java.time.LocalTime.now().hour
        when {
            hour < 6 -> "凌晨好"
            hour < 12 -> "早上好"
            hour < 14 -> "中午好"
            hour < 18 -> "下午好"
            else -> "晚上好"
        }
    }
    val todayLabel = remember {
        val today = LocalDate.now()
        val week = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[today.dayOfWeek.value % 7]
        today.format(DateTimeFormatter.ofPattern("M月d日")) + " · " + week
    }

    fun latestMetrics(records: List<MedicalRecord>): List<Triple<Metric, Metric?, Boolean>> {
        val byName = LinkedHashMap<String, MutableList<Metric>>()
        for (r in records) {
            if (r.metricsJson.isBlank()) continue
            runCatching { Json.decodeFromString<List<Metric>>(r.metricsJson) }
                .getOrElse { emptyList() }
                .forEach { m -> byName.getOrPut(m.name) { mutableListOf() }.add(m) }
        }
        return byName.mapNotNull { (_, list) ->
            val latest = list.lastOrNull() ?: return@mapNotNull null
            val prev = list.getOrNull(list.lastIndex - 1)
            Triple(latest, prev, latest.abnormal)
        }.take(3)
    }

    LaunchedEffect(selectedMemberId) {
        selectedMemberId?.let { id ->
            database.medicalRecordDao().getRecentRecordsByMember(id, 1)
                .catch { e -> error = e.message }
                .collect { records -> recentRecords = records }
            database.medicalRecordDao().getRecentRecordsByMember(id, 50)
                .catch { e -> error = e.message }
                .collect { records -> trendRecords = records }
        } ?: run {
            recentRecords = emptyList()
            trendRecords = emptyList()
        }
    }

    val currentMember = members.firstOrNull { it.id == selectedMemberId }
    val overviewMetrics = remember(trendRecords) { latestMetrics(trendRecords) }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = if (currentMember != null) "$greeting，${currentMember.name}" else "医迹",
                subtitle = todayLabel,
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                MemberSelector(
                    members = members,
                    selectedMemberId = selectedMemberId,
                    onSelect = { member ->
                        SelectedMemberHolder.homeSelectedMemberId.value = member.id
                        scope.launch {
                            database.userSettingsDao().getUserSettings().firstOrNull()?.let { s ->
                                database.userSettingsDao().insertOrUpdate(s.copy(selectedMemberId = member.id))
                            }
                        }
                    }
                )
            }

            item {
                SectionCard(title = "健康概览") {
                    if (overviewMetrics.isEmpty()) {
                        Text(
                            "暂无 AI 解析的健康指标。在添加记录时使用拍照识别 / 文本分析，AI 提取的指标会显示在这里。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            overviewMetrics.forEach { (metric, prev, abnormal) ->
                                val num = parseNumeric(metric.value)
                                val prevNum = prev?.let { parseNumeric(it.value) }
                                val changed = if (num != null && prevNum != null && prevNum != 0.0)
                                    ((num - prevNum) / prevNum) * 100 else null
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = AppShapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (abnormal)
                                            MaterialTheme.colorScheme.errorContainer
                                        else
                                            MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            metric.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (abnormal)
                                                MaterialTheme.colorScheme.onErrorContainer
                                            else
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            metric.value + if (metric.unit.isNotBlank()) " ${metric.unit}" else "",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (abnormal)
                                                MaterialTheme.colorScheme.onErrorContainer
                                            else
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        changed?.let {
                                            val pct = kotlin.math.abs(it).toInt()
                                            Text(
                                                (if (it >= 0) "▲ " else "▼ ") + "$pct%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (abnormal)
                                                    MaterialTheme.colorScheme.onErrorContainer
                                                else
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            currentMember?.let { member ->
                item {
                    val notes = buildList {
                        if (member.allergy.isNotBlank()) add("过敏史" to member.allergy)
                        if (member.chronic.isNotBlank()) add("慢性病" to member.chronic)
                        if (member.medicationNote.isNotBlank()) add("用药注意" to member.medicationNote)
                        if (member.otherNote.isNotBlank()) add("其他备注" to member.otherNote)
                    }
                    if (notes.isNotEmpty()) {
                        SectionCard(title = "医疗注意事项") {
                            notes.forEach { (label, value) -> InfoRow(label, value) }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最新医疗记录",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { navController.navigate("medical_records") }) {
                        Text("查看全部")
                    }
                }
            }

            if (recentRecords.isEmpty()) {
                item { EmptyRecords(onAdd = { navController.navigate("add_record") }) }
            } else {
                items(recentRecords) { record ->
                    MedicalRecordCard(record)
                }
            }

            item {
                val series = remember(trendRecords) { buildSeries(trendRecords) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.large,
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                    elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("健康趋势", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                        TrendSection(series = series)

                        Button(
                            onClick = { runAiAnalysis() },
                            enabled = !aiAnalyzing && members.any { it.id == selectedMemberId },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (aiAnalyzing) "AI 分析中…" else "AI 综合分析")
                        }
                        aiError?.let {
                            Text("分析失败：$it", color = MaterialTheme.colorScheme.error)
                        }
                        aiTrend?.let { trend ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "AI 趋势解读",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        trend,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
