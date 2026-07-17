package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    val selectedMemberId = SelectedMemberHolder.recordsSelectedMemberId.value
    val dayFmt = DateTimeFormatter.ofPattern("MM-dd")

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
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("AI 趋势解读", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.height(8.dp))
                        Text(trend, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }

            if (error != null) {
                Text("加载出错：$error", color = MaterialTheme.colorScheme.error)
            } else {
                TrendSection(series = series)
            }
        }
    }
}
