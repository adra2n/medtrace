package com.yy.chiyaole.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicalRecord
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicalRecordScreen(
    database: AppDatabase,
    navController: NavController
) {
    var patientName by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var dailyFrequency by remember { mutableStateOf(1) }
    var medicationTimes by remember { mutableStateOf(List(1) { LocalTime.of(8, 0) }) }
    var dosage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var onsetTime by remember { mutableStateOf(LocalDateTime.now()) }
    var error by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加医疗记录") },
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
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("患者姓名") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = { Text("诊断结果") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedButton(
                onClick = {
                    val currentDateTime = onsetTime
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    onsetTime = LocalDateTime.of(
                                        year, month + 1, dayOfMonth,
                                        hourOfDay, minute
                                    )
                                },
                                currentDateTime.hour,
                                currentDateTime.minute,
                                true
                            ).show()
                        },
                        currentDateTime.year,
                        currentDateTime.monthValue - 1,
                        currentDateTime.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, "选择日期时间")
                Spacer(Modifier.width(8.dp))
                Text("就诊时间：${onsetTime.format(dateTimeFormatter)}")
            }
            
            OutlinedTextField(
                value = medications,
                onValueChange = { medications = it },
                label = { Text("开具药品") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Text("每日服药次数", style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..4).forEach { count ->
                    OutlinedButton(
                        onClick = {
                            dailyFrequency = count
                            medicationTimes = List(count) { index ->
                                when (index) {
                                    0 -> LocalTime.of(8, 0)  // 早上8点
                                    1 -> LocalTime.of(12, 0) // 中午12点
                                    2 -> LocalTime.of(18, 0) // 晚上6点
                                    else -> LocalTime.of(21, 0) // 睡前9点
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (dailyFrequency == count) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(count.toString())
                    }
                }
            }

            Text("服药时间", style = MaterialTheme.typography.bodyLarge)
            medicationTimes.forEachIndexed { index, time ->
                OutlinedButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                medicationTimes = medicationTimes.toMutableList().apply {
                                    this[index] = LocalTime.of(hourOfDay, minute)
                                }
                            },
                            time.hour,
                            time.minute,
                            true
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("第${index + 1}次：${time.format(DateTimeFormatter.ofPattern("HH:mm"))}")
                }
            }
            
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("用药剂量（如：每次一片）") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (patientName.isBlank() || diagnosis.isBlank() || 
                            medications.isBlank() || dosage.isBlank()
                        ) {
                            error = "请填写必要信息"
                            return@Button
                        }
                        
                        val frequency = "每天${dailyFrequency}次：" + medicationTimes.joinToString(", ") { 
                            it.format(DateTimeFormatter.ofPattern("HH:mm")) 
                        }
                        
                        val record = MedicalRecord(
                            id = 0,
                            patientName = patientName,
                            diagnosis = diagnosis,
                            onsetTime = onsetTime,
                            medications = medications,
                            frequency = frequency,
                            dosage = dosage,
                            notes = notes
                        )
                        
                        scope.launch {
                            database.medicalRecordDao().insert(record)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
                
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
            }
        }
    }
}
