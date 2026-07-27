package com.yy.medtrace.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.ui.components.MemberSelector
import com.yy.medtrace.ui.components.TrendSection
import com.yy.medtrace.ui.components.buildSeries
import com.yy.medtrace.ui.state.SelectedMemberHolder
import com.yy.medtrace.ui.theme.GradientTopBar
import com.yy.medtrace.ui.theme.Primary
import com.yy.medtrace.ui.theme.AppShapes
import com.yy.medtrace.ui.theme.SoftElevation
import com.yy.medtrace.ui.theme.cardContainerColor
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    val selectedMemberId = SelectedMemberHolder.selectedMemberId.value

    val periods = listOf(
        "7天" to 7L,
        "30天" to 30L,
        "半年" to 180L,
        "全年" to 365L
    )
    var selectedPeriodDays by remember { mutableStateOf(180L) }

    LaunchedEffect(Unit) {
        database.familyMemberDao().getAllMembers()
            .catch { e -> error = e.message }
            .collect { list ->
                members = list + com.yy.medtrace.ui.state.UNKNOWN_MEMBER
                if (SelectedMemberHolder.selectedMemberId.value == null && list.isNotEmpty()) {
                    SelectedMemberHolder.selectedMemberId.value = list.first().id
                }
            }
    }

    LaunchedEffect(selectedMemberId) {
        selectedMemberId?.let { id ->
            val flow = if (id == 0L) {
                database.medicalRecordDao().getUnknownRecords()
            } else {
                database.medicalRecordDao().getRecordsByMember(id)
            }
            flow
                .catch { e -> error = e.message }
                .collectLatest { records = it }
        } ?: run { records = emptyList() }
    }

    val series = remember(records, selectedPeriodDays) {
        val cutoff = LocalDateTime.now().minusDays(selectedPeriodDays)
        buildSeries(records.filter { (it.onsetTime ?: LocalDateTime.MIN) >= cutoff })
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "数据统计",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
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
                    onSelect = { member -> scope.launch { SelectedMemberHolder.select(member.id, database) } },
                    emptyHint = "暂无家庭成员，请先在家庭中添加"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.forEach { (label, days) ->
                    FilterChip(
                        selected = selectedPeriodDays == days,
                        onClick = { selectedPeriodDays = days },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (error != null) {
                Text("加载出错：$error", color = MaterialTheme.colorScheme.error)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppShapes.large,
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                    elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        TrendSection(series = series)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(androidx.compose.ui.graphics.Color(0xFF2E9E5B))
                            )
                            Text("绿色=正常", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Text("橙色=指标偏高", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 记录统计
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.large,
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "记录统计",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${records.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            Text(
                                text = "总记录",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val recentCount = records.count {
                                it.onsetTime?.isAfter(LocalDateTime.now().minusDays(30)) ?: false
                            }
                            Text(
                                text = "$recentCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            Text(
                                text = "近30天",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${records.sumOf { it.medItems.size }}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            Text(
                                text = "药品数",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
