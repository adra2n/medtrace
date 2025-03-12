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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.WorkManager
//import androidx.work.WorkManager
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.data.model.MedicationStatus
import com.yy.chiyaole.util.ReminderScheduler
//import com.yy.chiyaole.util.ScheduleMedicationReminder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationReminderScreen(
    database: AppDatabase,
    navController: NavController,
//    workManager: WorkManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reminders by remember { mutableStateOf<List<MedicationReminder>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

//     加载用户设置和提醒列表
    LaunchedEffect(Unit) {
        // 加载用户设置
        database.userSettingsDao().getUserSettings().collect { settings ->
            // 加载提醒列表
            database.medicationReminderDao().getAll()
                .catch { e ->
                    error = e.message
                    e.printStackTrace()
                }
                .collectLatest { reminderList ->
                    reminders = reminderList

                    // 重新调度所有活跃的提醒
//                    reminderList.forEach { reminder ->
//                        if (reminder.isActive) {
//                            ReminderScheduler.scheduleReminder(
//                                context = context,
//                                reminder = reminder,
//                                advanceMinutes = settings?.reminderAdvanceMinutes ?: 30
//                            )
//                        }
//                    }
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用药提醒") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_reminder") }
            ) {
                Icon(Icons.Default.Add, "添加提醒")
            }
        }
    ) { padding ->
        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("加载数据时出错：$error")
            }
        } else if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有添加任何用药提醒")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(reminders) { reminder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
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
                                Text(
                                    text = reminder.patientName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            navController.navigate("edit_reminder/${reminder.id}")
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, "编辑")
                                    }
                                    // 如果是删除的话，将取消所有提醒任务
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    // 取消提醒调度
//                                                    ReminderScheduler.cancelReminder(context, reminder.id)
                                                    val workManager = WorkManager.getInstance(context)
                                                    workManager.cancelAllWorkByTag("reminder_${reminder.id}")
                                                    // 从数据库删除
                                                    database.medicationReminderDao().delete(reminder)
                                                } catch (e: Exception) {
                                                    error = e.message
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, "删除")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "药品名称：${reminder.medicineName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "开始时间：${reminder.startDate.format(dateFormatter)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "结束时间：${reminder.endDate.format(dateFormatter)}",
                                style = MaterialTheme.typography.bodyMedium
                            )

//                            getNextDoseTime(reminder)?.let { nextDoseTime ->
//                                Text(
//                                    text = "下次服药时间：${nextDoseTime.format(dateFormatter)}",
//                                    style = MaterialTheme.typography.bodyMedium
//                                )
//                            } ?: Text(
//                                text = "已完成服药",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.secondary
//                            )
                            
                            Text(
                                text = "每天服用次数：${reminder.timesPerDay}次",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = "服药时间：${reminder.medicationTimes.joinToString(", ") { it.format(timeFormatter) }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "用药剂量：每次 ${reminder.dosageAmount} ${reminder.dosageUnit}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            if (reminder.instructions.isNotBlank()) {
                                Text(
                                    text = "服药说明：${reminder.instructions}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}