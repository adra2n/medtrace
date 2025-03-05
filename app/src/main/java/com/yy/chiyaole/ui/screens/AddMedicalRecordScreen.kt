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
    var frequency by remember { mutableStateOf("") }
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
            
            OutlinedTextField(
                value = frequency,
                onValueChange = { frequency = it },
                label = { Text("服药频率（如：每天三次）") },
                modifier = Modifier.fillMaxWidth()
            )
            
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
                            medications.isBlank() || frequency.isBlank() || dosage.isBlank()
                        ) {
                            error = "请填写必要信息"
                            return@Button
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
