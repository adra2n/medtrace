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
//import com.yy.chiyaole.util.ComplianceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class MedicationStats(
    val totalToday: Int = 0,
    val completedToday: Int = 0
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
                    
                    medicationStats = MedicationStats(
                        totalToday = totalDoses,
                        completedToday = completedDoses
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
            item {
                Text(
                    text = "用药统计",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
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
                        }
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
                    ReminderCard(
                        reminder = reminder,
                        timeFormatter = timeFormatter,
                        now = now,
                        database = database
                    )
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
    now: LocalDateTime,
    database: AppDatabase,
    modifier: Modifier = Modifier
) {
    var todayRecords by remember { mutableStateOf<List<MedicationRecord>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // 获取今日用药记录
    LaunchedEffect(reminder.id) {
        database.medicationRecordDao().getReminderDayRecords(
            reminderId = reminder.id,
            date = LocalDateTime.now()
        ).collect { records ->
            todayRecords = records
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = reminder.medicineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "服用剂量：${reminder.dosageAmount}${reminder.dosageUnit}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                calculateNextDoseTime(reminder, now)?.let { nextDoseTime ->
                    Text(
                        text = "下次：${nextDoseTime.format(timeFormatter)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 显示今日用药记录
            if (todayRecords.isNotEmpty()) {
                Text(
                    text = "今日服药记录",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                todayRecords.forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "计划时间：${record.scheduledTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = when (record.status) {
                                MedicationStatus.TAKEN -> "已服用"
                                MedicationStatus.SKIPPED -> "已跳过"
                                MedicationStatus.DELAYED -> "已延迟"
                                MedicationStatus.PENDING -> "待服用"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (record.status) {
                                MedicationStatus.TAKEN -> MaterialTheme.colorScheme.primary
                                MedicationStatus.SKIPPED -> MaterialTheme.colorScheme.error
                                MedicationStatus.DELAYED -> MaterialTheme.colorScheme.tertiary
                                MedicationStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    record.actualTime?.let { actualTime ->
                        Text(
                            text = "实际服药时间：${actualTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 添加快速服药按钮
                if (reminder.isActive && !now.isAfter(reminder.endDate)) {
                    val pendingRecords = todayRecords.filter { it.status == MedicationStatus.PENDING }
                    if (pendingRecords.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val record = pendingRecords.first()
                                        val updatedRecord = record.copy(
                                            status = MedicationStatus.TAKEN,
                                            actualTime = LocalDateTime.now()
                                        )
                                        database.medicationRecordDao().update(updatedRecord)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("已服用")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val record = pendingRecords.first()
                                        val updatedRecord = record.copy(
                                            status = MedicationStatus.SKIPPED,
                                            actualTime = LocalDateTime.now()
                                        )
                                        database.medicationRecordDao().update(updatedRecord)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("跳过")
                            }
                        }
                    }
                }
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
