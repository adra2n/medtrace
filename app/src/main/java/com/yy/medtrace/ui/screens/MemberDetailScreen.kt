package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.llm.ComprehensiveAnalysisUseCase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.settings.LlmSettingsStore
import com.yy.medtrace.ui.components.EmptyRecords
import com.yy.medtrace.ui.components.MemberAvatar
import com.yy.medtrace.ui.components.MemberEditDialog
import com.yy.medtrace.ui.components.MedicalRecordCard
import com.yy.medtrace.ui.components.SectionCard
import com.yy.medtrace.ui.components.buildSeries
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import com.yy.medtrace.ui.theme.computeAge
import com.yy.medtrace.ui.theme.memberCardColors
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

private enum class DetailTab(val label: String) {
    Medication("用药记录"),
    Exam("检查报告"),
    Metric("健康指标"),
    Visit("就诊记录")
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemberDetailScreen(
    database: AppDatabase,
    navController: NavController,
    memberId: Long,
    initialTab: String? = null
) {
    var member by remember { mutableStateOf<FamilyMember?>(null) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var selectedTab by remember {
        mutableStateOf(
            if (initialTab == "check") DetailTab.Exam else DetailTab.Medication
        )
    }
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var aiAdvice by remember { mutableStateOf<String?>(null) }
    var aiAnalyzing by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val llmSettings = remember { LlmSettingsStore(context) }

    LaunchedEffect(memberId) {
        scope.launch {
            SelectedMemberHolder.select(memberId, database)
            member = database.familyMemberDao().getMemberById(memberId)
        }
        database.medicalRecordDao().getRecordsByMember(memberId)
            .catch { }
            .collect { records = it }
    }

    val medicationRecords = remember(records) { records.filter { it.medItems.isNotEmpty() } }
    val examRecords = remember(records) { records.filter { it.metricsJson.isNotBlank() } }
    val visitRecords = remember(records) { records }
    val metricPoints = remember(records) {
        buildSeries(records).flatMap { series ->
            series.points.map { point ->
                MetricView(
                    name = series.name,
                    value = point.raw,
                    unit = series.unit,
                    abnormal = point.abnormal,
                    time = point.time
                )
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = member?.let {
                    val age = computeAge(it.birthday)?.let { a -> "，${a}岁" } ?: ""
                    "${it.name}$age"
                } ?: "成员档案",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showEdit = true }) {
                        Text("编辑档案", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (aiAnalyzing) {
                        aiJob?.cancel()
                        return@FloatingActionButton
                    }
                    val m = member ?: return@FloatingActionButton
                    aiError = null
                    aiAnalyzing = true
                    aiJob = scope.launch {
                        try {
                            val result = ComprehensiveAnalysisUseCase(llmSettings)
                                .analyze(m, records)
                            aiSummary = result.trend.ifBlank { result.raw }
                            aiAdvice = result.advice.ifBlank { null }
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) return@launch
                            aiError = e.message ?: "分析失败"
                        } finally {
                            aiAnalyzing = false
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.AutoAwesome, "AI健康汇总", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    member?.let {
                        MemberAvatar(member = it, size = 64.dp)
                        Column {
                            Text(it.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            val age = computeAge(it.birthday)
                            Text(
                                listOfNotNull(
                                    it.relation.ifBlank { null },
                                    age?.let { a -> "${a}岁" }
                                ).joinToString(" · ").ifBlank { "暂无资料" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (aiError != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            "AI 分析失败：$aiError",
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            aiSummary?.let {
                item {
                    SectionCard(title = "AI 健康汇总") {
                        if (aiAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            if (aiAdvice != null) {
                                Text("健康建议", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(4.dp))
                                Text(aiAdvice!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(10.dp))
                            }
                            Text("趋势解读", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailTab.values().forEach { tab ->
                        val selected = selectedTab == tab
                        val count = when (tab) {
                            DetailTab.Medication -> medicationRecords.size
                            DetailTab.Exam -> examRecords.size
                            DetailTab.Metric -> metricPoints.size
                            DetailTab.Visit -> visitRecords.size
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { selectedTab = tab },
                            label = { Text("${tab.label} $count") }
                        )
                    }
                }
            }

            when (selectedTab) {
                DetailTab.Medication -> {
                    if (medicationRecords.isEmpty()) item { EmptyRecords() }
                    else items(medicationRecords) { MedicalRecordCard(it) }
                }
                DetailTab.Exam -> {
                    if (examRecords.isEmpty()) item { EmptyRecords() }
                    else items(examRecords) { MedicalRecordCard(it) }
                }
                DetailTab.Metric -> {
                    if (metricPoints.isEmpty()) item { EmptyRecords() }
                    else items(metricPoints) { mv ->
                        MetricRowCard(mv)
                    }
                }
                DetailTab.Visit -> {
                    if (visitRecords.isEmpty()) item { EmptyRecords() }
                    else items(visitRecords) { MedicalRecordCard(it) }
                }
            }
        }
    }

    if (showEdit && member != null) {
        MemberEditDialog(
            member = member,
            onDismiss = { showEdit = false },
            onSave = { m ->
                scope.launch {
                    database.familyMemberDao().update(m)
                    member = database.familyMemberDao().getMemberById(m.id)
                }
                showEdit = false
            }
        )
    }
}

private data class MetricView(
    val name: String,
    val value: String,
    val unit: String,
    val abnormal: Boolean,
    val time: java.time.LocalDateTime
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MetricRowCard(mv: MetricView) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (mv.abnormal)
                MaterialTheme.colorScheme.errorContainer else cardContainerColor()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(mv.name, style = MaterialTheme.typography.titleSmall,
                    color = if (mv.abnormal) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
                Text(
                    mv.time.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mv.abnormal) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${mv.value}${if (mv.unit.isNotBlank()) " " + mv.unit else ""}",
                style = MaterialTheme.typography.titleMedium,
                color = if (mv.abnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
