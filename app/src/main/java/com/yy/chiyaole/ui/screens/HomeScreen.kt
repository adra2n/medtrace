package com.yy.chiyaole.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.model.MedicationStatus
import com.yy.chiyaole.ui.components.EmptyRecords
import com.yy.chiyaole.ui.components.EmptyReminders
import com.yy.chiyaole.ui.components.HealthTipsCard
import com.yy.chiyaole.ui.components.MedicalRecordCard
import com.yy.chiyaole.ui.components.ReminderCard
import com.yy.chiyaole.ui.components.StatItem
//import com.yy.chiyaole.util.ComplianceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MedicationStats(
    val totalToday: Int = 0,
    val completedToday: Int = 0,
//    val totalDosesToday: Int = 0,    // 今日总服药次数
    val takenDosesToday: Int = 0     // 今日已服用次数
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    database: AppDatabase,
    navController: NavController
) {
    var todayReminders by remember { mutableStateOf<List<MedicationReminder>>(emptyList()) }
    var recentRecords by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
//    var medicationRecords by remember { mutableStateOf<List<MedicationRecord>>(emptyList()) }
    var medicationStats by remember { mutableStateOf(MedicationStats()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
//    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val now = LocalDateTime.now()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 加载数据
    LaunchedEffect(Unit, now) {
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
                    
                    // 计算今日服药次数
                    val todayRecords = records.filter { 
                        it.scheduledTime.toLocalDate() == now.toLocalDate()
                    }
                    val takenDosesToday = todayRecords.count { 
                        it.status == MedicationStatus.TAKEN 
                    }
                    
                    medicationStats = MedicationStats(
                        totalToday = totalDoses,
                        completedToday = takenDosesToday,
                        takenDosesToday = takenDosesToday
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
                    text = "今日用药统计",
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatItem(
                                title = "服药数量",
                                value = "${medicationStats.totalToday}",
                                icon = Icons.Default.Notifications
                            )
//                            StatItem(
//                                title = "今日次数",
//                                value = "${medicationStats.totalDosesToday}",
//                                icon = Icons.Default.Done
//                            )
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
                        timeFormatter = dateTimeFormatter,
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
