package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.ui.theme.cardContainerColor
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    database: AppDatabase,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<MedicalRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    LaunchedEffect(Unit) {
        database.medicalRecordDao().getAllRecords()
            .catch { e ->
                error = e.message
                e.printStackTrace()
            }
            .collectLatest {
                records = it
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("医疗记录") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_record") }
            ) {
                Icon(Icons.Default.Add, "添加记录")
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
        } else if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("还没有添加任何医疗记录")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(records) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor())
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
                                    text = record.patientName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row {
                                    IconButton(
                                        onClick = { navController.navigate("add_record/${record.id}") }
                                    ) {
                                        Icon(Icons.Default.Edit, "编辑")
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    database.medicalRecordDao().delete(record)
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
                                text = "诊断结果：${record.diagnosis}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "就诊时间：${record.onsetTime.format(dateFormatter)}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (record.hospital.isNotBlank()) {
                                Text(
                                    text = "就诊医院：${record.hospital}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (record.medItems.isNotEmpty()) {
                                Text(
                                    text = "开具药品：",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                record.medItems.forEach { med ->
                                    val parts = listOf(med.name, med.dose, med.freq, med.duration)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" ")
                                    Text(
                                        text = "· $parts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            if (record.notes.isNotBlank()) {
                                Text(
                                    text = "备注：${record.notes}",
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
