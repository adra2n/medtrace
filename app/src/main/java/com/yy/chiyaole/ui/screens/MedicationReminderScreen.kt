package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.WorkManager
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationReminder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationReminderScreen(
    database: AppDatabase,
    navController: NavController,
    workManager: WorkManager
) {
    val scope = rememberCoroutineScope()
    var reminders by remember { mutableStateOf<List<MedicationReminder>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    LaunchedEffect(Unit) {
        database.medicationReminderDao().getAll()
            .catch { e ->
                error = e.message
                e.printStackTrace()
            }
            .collectLatest {
                reminders = it
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用药提醒") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(Icons.Default.Home, "返回主页")
                    }
                }
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
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    // 取消提醒
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
                            
                            Text(
                                text = "第一次服药时间：${reminder.firstDoseTime.format(dateFormatter)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "服药间隔：${reminder.intervalHours}小时",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "服药频率：${reminder.frequency}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "用药剂量：${reminder.dosage}",
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
