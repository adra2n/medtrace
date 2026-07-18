package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.llm.ComprehensiveAnalysisUseCase
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.settings.LlmSettingsStore
import com.yy.chiyaole.ui.components.MemberSelector
import com.yy.chiyaole.ui.components.TrendSection
import com.yy.chiyaole.ui.components.buildSeries
import com.yy.chiyaole.ui.state.SelectedMemberHolder
import com.yy.chiyaole.ui.theme.GradientTopBar
import com.yy.chiyaole.ui.theme.Primary
import com.yy.chiyaole.ui.theme.AppShapes
import com.yy.chiyaole.ui.theme.SoftElevation
import com.yy.chiyaole.ui.theme.cardContainerColor
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
    val context = LocalContext.current
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var aiTrend by remember { mutableStateOf<String?>(null) }
    var aiAnalyzing by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }
    val selectedMemberId = SelectedMemberHolder.selectedMemberId.value
    val dayFmt = DateTimeFormatter.ofPattern("MM-dd")

    val periods = listOf(
        "7天" to 7L,
        "30天" to 30L,
        "半年" to 180L,
        "全年" to 365L
    )
    var selectedPeriodDays by remember { mutableStateOf(180L) }

    fun runAiAnalysis() {
        val member = members.firstOrNull { it.id == selectedMemberId } ?: return
        aiError = null
        aiAnalyzing = true
        scope.launch {
            try {
                val result = ComprehensiveAnalysisUseCase(LlmSettingsStore(context))
                    .analyze(member, records)
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
                members = list
                if (SelectedMemberHolder.selectedMemberId.value == null && list.isNotEmpty()) {
                    SelectedMemberHolder.selectedMemberId.value = list.first().id
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

    val series = remember(records, selectedPeriodDays) {
        val cutoff = LocalDateTime.now().minusDays(selectedPeriodDays)
        buildSeries(records.filter { (it.onsetTime ?: LocalDateTime.MIN) >= cutoff })
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "健康分析",
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
                    onSelect = { member -> scope.launch { SelectedMemberHolder.select(member.id, database) } },
                    emptyHint = "暂无家庭成员，请先在家庭中添加"
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.large,
                colors = CardDefaults.cardColors(containerColor = cardContainerColor()),
                elevation = CardDefaults.cardElevation(defaultElevation = SoftElevation)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AutoAwesome, "AI 健康概览", tint = Primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("AI 健康概览", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("基于近期健康指标智能总结", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = { runAiAnalysis() },
                        enabled = !aiAnalyzing && members.any { it.id == selectedMemberId },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(if (aiAnalyzing) "AI 分析中…" else "生成健康概览", color = MaterialTheme.colorScheme.onPrimary)
                    }

                    aiError?.let {
                        Text("分析失败：$it", color = MaterialTheme.colorScheme.error)
                    }

                    aiTrend?.let { trend ->
                        Surface(
                            shape = AppShapes.medium,
                            color = Primary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(trend, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("AI 生成内容仅供参考，不替代专业医疗诊断", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
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

            Button(
                onClick = {
                    android.widget.Toast.makeText(
                        context,
                        "PDF 病历导出功能开发中",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("导出 PDF 完整病历", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
