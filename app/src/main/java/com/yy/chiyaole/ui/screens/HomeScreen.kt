package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.model.MedicationRecord
import com.yy.chiyaole.data.model.MedicationStatus
import com.yy.chiyaole.util.ComplianceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class MedicationStats(
    val totalToday: Int = 0,
    val completedToday: Int = 0,
    val weeklyAdherence: Float = 0f,
    val monthlyAdherence: Float = 0f,
    val complianceAdvice: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    database: AppDatabase,
    navController: NavController
) {
    var todayReminders by remember { mutableStateOf<List<MedicationReminder>>(emptyList()) }
    var recentRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var medicationRecords by remember { mutableStateOf<List<MedicationRecord>>(emptyList()) }
    var medicationStats by remember { mutableStateOf(MedicationStats()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val now = LocalDateTime.now()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 加载数据
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                // 合并提醒和记录数据流
                combine(
                    database.medicationReminderDao().getTodayReminders(now),
                    database.medicationRecordDao().getAll()
                ) { reminders, records ->
                    todayReminders = reminders
                    
                    // 计算用药统计
                    val totalDoses = reminders.sumOf { it.timesPerDay }
                    val completedDoses = records.count { 
                        it.scheduledTime.toLocalDate() == now.toLocalDate() && 
                        it.status == MedicationStatus.TAKEN
                    }
                    
                    // 计算依从率
                    val weeklyAdherence = ComplianceUtil.calculateWeeklyComplianceRate(records)
                    val monthlyAdherence = ComplianceUtil.calculateMonthlyComplianceRate(records)
                    val complianceAdvice = ComplianceUtil.getComplianceAdvice(weeklyAdherence)
                    
                    medicationStats = MedicationStats(
                        totalToday = totalDoses,
                        completedToday = completedDoses,
                        weeklyAdherence = weeklyAdherence,
                        monthlyAdherence = monthlyAdherence,
                        complianceAdvice = complianceAdvice
                    )
                }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    error = e.message
                    e.printStackTrace()
                }
                .collect()
            }
            
            launch {
                // 加载最近的医疗记录
                database.medicalRecordDao().getRecentRecords(3)
                    .flowOn(Dispatchers.Default)
                    .catch { e ->
                        error = e.message
                        e.printStackTrace()
                    }
                    .collect { records ->
                        recentRecords = records
                    }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智药乐") },
                actions = {
                    IconButton(
                        onClick = { showSettings = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置"
                        )
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
            // 用药统计卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "今日用药统计",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatItem(
                                title = "今日药品",
                                value = "${medicationStats.totalToday}",
                                icon = Icons.Default.Notifications
                            )
                            StatItem(
                                title = "已服用",
                                value = "${medicationStats.completedToday}",
                                icon = Icons.Default.CheckCircle
                            )
                            StatItem(
                                title = "本周依从率",
                                value = "${(medicationStats.weeklyAdherence * 100).toInt()}%",
                                icon = Icons.Default.List
                            )
                        }
                        
                        // 依从率建议
                        if (medicationStats.complianceAdvice.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = medicationStats.complianceAdvice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // 月度依从率卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "月度依从率",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${(medicationStats.monthlyAdherence * 100).toInt()}%",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // 今日用药提醒
            item {
                Text(
                    text = "今日用药提醒",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (todayReminders.isEmpty()) {
                item {
                    EmptyReminders()
                }
            } else {
                items(todayReminders) { reminder ->
                    ReminderCard(reminder, timeFormatter, now)
                }
            }

            // 最近医疗记录
            item {
                Text(
                    text = "最近医疗记录",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (recentRecords.isEmpty()) {
                item {
                    EmptyRecords()
                }
            } else {
                items(recentRecords) { record ->
                    MedicalRecordCard(record)
                }
            }

            // 健康提示
            item {
                HealthTipsCard()
            }
        }
    }
    
    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun EmptyReminders() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "没有提醒",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "今天没有需要服用的药物",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyRecords() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "没有记录",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "暂无医疗记录",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ReminderCard(
    reminder: MedicationReminder,
    timeFormatter: DateTimeFormatter,
    now: LocalDateTime
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.patientName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "每天 ${reminder.timesPerDay} 次",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = "药品：${reminder.medicineName}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "剂量：每次 ${reminder.dosageAmount} ${reminder.dosageUnit}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Text(
            //     text = "服药时间：${reminder.medicationTimes.joinToString(", ") { it.format(timeFormatter) }}",
            //     style = MaterialTheme.typography.bodyMedium
            // )
            
            val nextDoseTime = calculateNextDoseTime(reminder, now)
            if (nextDoseTime != null) {
                Text(
                    text = "服药时间：${nextDoseTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (reminder.instructions.isNotBlank()) {
                Text(
                    text = "说明：${reminder.instructions}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun MedicalRecordCard(record: MedicalRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.patientName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = record.onsetTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = "诊断：${record.diagnosis}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "用药：${record.medications}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (record.notes.isNotBlank()) {
                Text(
                    text = "备注：${record.notes}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun HealthTipsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "健康小贴士",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Text(
                text = "定时服药的重要性：按时服药不仅能保证药物疗效，还能预防疾病复发。建议设置手机提醒，帮助您准时服药。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun calculateNextDoseTime(reminder: MedicationReminder, now: LocalDateTime): LocalTime? {
    // 获取今天的所有服药时间点
    val todayTimes = reminder.medicationTimes.map { time ->
        LocalDateTime.of(now.toLocalDate(), time)
    }
    
    // 找到下一个服药时间点
    return todayTimes.find { it.isAfter(now) }?.toLocalTime()
}
