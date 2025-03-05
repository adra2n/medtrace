package com.yy.chiyaole.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.*
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.worker.MedicationReminderWorker
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationReminderScreen(
    database: AppDatabase,
    navController: NavController,
    workManager: WorkManager,
    reminderId: Long?
) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    // 表单状态
    var patientName by remember { mutableStateOf("") }
    var medicineName by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDateTime.now()) }
    var endDate by remember { mutableStateOf(LocalDateTime.now().plusDays(7)) }
    var firstDoseTime by remember { mutableStateOf(LocalDateTime.now()) }
    var intervalHours by remember { mutableStateOf("8") }
    var frequency by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    
    // 如果是编辑模式，加载现有数据
    LaunchedEffect(reminderId) {
        if (reminderId != null) {
            try {
                val reminder = database.medicationReminderDao().getById(reminderId)
                reminder?.let {
                    patientName = it.patientName
                    medicineName = it.medicineName
                    startDate = it.startDate
                    endDate = it.endDate
                    firstDoseTime = it.firstDoseTime
                    intervalHours = it.intervalHours.toString()
                    frequency = it.frequency
                    dosage = it.dosage
                    instructions = it.instructions
                }
            } catch (e: Exception) {
                error = e.message
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (reminderId == null) "添加用药提醒" else "编辑用药提醒") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
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
                value = medicineName,
                onValueChange = { medicineName = it },
                label = { Text("药品名称") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 开始日期选择
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            startDate = startDate.withYear(year)
                                .withMonth(month + 1)
                                .withDayOfMonth(dayOfMonth)
                            if (endDate.isBefore(startDate)) {
                                endDate = startDate.plusDays(7)
                            }
                        },
                        startDate.year,
                        startDate.monthValue - 1,
                        startDate.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, "选择日期")
                Spacer(Modifier.width(8.dp))
                Text("开始日期：${startDate.format(dateFormatter)}")
            }
            
            // 结束日期选择
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val newEndDate = endDate.withYear(year)
                                .withMonth(month + 1)
                                .withDayOfMonth(dayOfMonth)
                            if (!newEndDate.isBefore(startDate)) {
                                endDate = newEndDate
                            } else {
                                error = "结束日期不能早于开始日期"
                            }
                        },
                        endDate.year,
                        endDate.monthValue - 1,
                        endDate.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, "选择日期")
                Spacer(Modifier.width(8.dp))
                Text("结束日期：${endDate.format(dateFormatter)}")
            }
            
            // 第一次服药时间选择
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            firstDoseTime = firstDoseTime
                                .withHour(hourOfDay)
                                .withMinute(minute)
                        },
                        firstDoseTime.hour,
                        firstDoseTime.minute,
                        true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("第一次服药时间：${firstDoseTime.format(timeFormatter)}")
            }
            
            // 服药间隔
            OutlinedTextField(
                value = intervalHours,
                onValueChange = { 
                    if (it.isEmpty() || it.toIntOrNull() != null) {
                        intervalHours = it
                    }
                },
                label = { Text("服药间隔（小时）") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = frequency,
                onValueChange = { frequency = it },
                label = { Text("服药频率") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = { Text("用药剂量") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("服药说明") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    scope.launch {
                        try {
                            if (patientName.isBlank() || medicineName.isBlank() || 
                                frequency.isBlank() || dosage.isBlank() || intervalHours.isBlank()
                            ) {
                                error = "请填写必要信息"
                                return@launch
                            }
                            
                            val intervalHoursInt = intervalHours.toIntOrNull()
                            if (intervalHoursInt == null || intervalHoursInt <= 0) {
                                error = "请输入有效的服药间隔时间"
                                return@launch
                            }
                            
                            val reminder = MedicationReminder(
                                id = reminderId ?: 0,
                                patientName = patientName,
                                medicineName = medicineName,
                                startDate = startDate,
                                endDate = endDate,
                                firstDoseTime = firstDoseTime,
                                intervalHours = intervalHoursInt,
                                frequency = frequency,
                                dosage = dosage,
                                instructions = instructions
                            )

                            if (reminderId == null) {
                                // 添加新提醒
                                val id = database.medicationReminderDao().insert(reminder)
                                scheduleReminder(workManager, reminder.copy(id = id))
                            } else {
                                // 更新现有提醒
                                database.medicationReminderDao().update(reminder)
                                // 取消旧的提醒并创建新的
                                workManager.cancelAllWorkByTag("reminder_$reminderId")
                                scheduleReminder(workManager, reminder)
                            }

                            navController.popBackStack()
                        } catch (e: Exception) {
                            error = e.message
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (reminderId == null) "添加提醒" else "保存修改")
            }
        }
    }
}

private fun scheduleReminder(workManager: WorkManager, reminder: MedicationReminder) {
    val now = LocalDateTime.now()
    val initialDelay = ChronoUnit.MINUTES.between(now, reminder.firstDoseTime)
    
    val reminderRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
        reminder.intervalHours.toLong(),
        TimeUnit.HOURS
    )
        .setInitialDelay(initialDelay, TimeUnit.MINUTES)
        .addTag("reminder_${reminder.id}")
        .setInputData(
            workDataOf(
                "reminder_id" to reminder.id,
                "patient_name" to reminder.patientName,
                "medicine_name" to reminder.medicineName,
                "dosage" to reminder.dosage,
                "instructions" to reminder.instructions
            )
        )
        .build()

    workManager.enqueue(reminderRequest)
}
