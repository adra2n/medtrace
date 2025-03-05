package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationReminder
import kotlinx.coroutines.flow.catch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    database: AppDatabase,
    navController: NavController
) {
    var todayReminders by remember { mutableStateOf<List<MedicationReminder>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    LaunchedEffect(Unit) {
        database.medicationReminderDao().getTodayReminders(LocalDateTime.now())
            .catch { e ->
                error = e.message
                e.printStackTrace()
            }
            .collect { reminders ->
                todayReminders = reminders
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智药乐") }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { navController.navigate("medication_reminders") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.DateRange, "用药提醒")
                            Text("用药提醒")
                        }
                    }
                    
                    Button(
                        onClick = { navController.navigate("medical_records") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Home, "医疗记录")
                            Text("医疗记录")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "今日用药提醒",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            if (error != null) {
                Text(
                    text = "加载数据时出错：$error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (todayReminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
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
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(todayReminders) { reminder ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
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
                                        text = reminder.firstDoseTime.format(dateTimeFormatter),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                
                                Text(
                                    text = "药品：${reminder.medicineName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Text(
                                    text = "剂量：${reminder.dosage}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                if (reminder.instructions.isNotBlank()) {
                                    Text(
                                        text = "说明：${reminder.instructions}",
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
}
