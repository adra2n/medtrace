package com.yy.medtrace.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
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
    var showEdit by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
        buildSeries(records).flatMap { series ->
            series.points.map { point ->
                MetricView(
                    name = series.name,
                    value = point.raw,
                    unit = series.unit,
                    abnormal = point.abnormal,
                    time = point.time.format(formatter)
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp)
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

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }

            when (selectedTab) {
                DetailTab.Medication -> {
                    if (medicationRecords.isEmpty()) {
                        item { EmptyRecords() }
                    } else {
                        items(medicationRecords, key = { it.id }) { record ->
                            MedicalRecordCard(record)
                        }
                    }
                }
                DetailTab.Exam -> {
                    if (examRecords.isEmpty()) {
                        item { EmptyRecords() }
                    } else {
                        items(examRecords, key = { it.id }) { record ->
                            MedicalRecordCard(record)
                        }
                    }
                }
                DetailTab.Metric -> {
                    if (metricPoints.isEmpty()) {
                        item { EmptyRecords() }
                    } else {
                        items(metricPoints, key = { "${it.name}-${it.time}" }) { point ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppShapes.medium,
                                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                                elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(point.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(point.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        "${point.value} ${point.unit}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (point.abnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                DetailTab.Visit -> {
                    if (visitRecords.isEmpty()) {
                        item { EmptyRecords() }
                    } else {
                        items(visitRecords, key = { it.id }) { record ->
                            MedicalRecordCard(record)
                        }
                    }
                }
            }
        }
    }

    if (showEdit) {
        MemberEditDialog(
            member = member,
            onDismiss = { showEdit = false },
            onSave = { m ->
                scope.launch {
                    if (member == null) database.familyMemberDao().insert(m)
                    else database.familyMemberDao().update(m)
                    member = m
                }
                showEdit = false
            }
        )
    }
}

data class MetricView(
    val name: String,
    val value: String,
    val unit: String,
    val abnormal: Boolean,
    val time: String
)
