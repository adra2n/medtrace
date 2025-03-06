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
import java.time.Duration

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
    var timesPerDay by remember { mutableStateOf("1") }
    var dosageAmount by remember { mutableStateOf("1") }
    var dosageUnit by remember { mutableStateOf("片") }
    var instructions by remember { mutableStateOf("") }
    
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dosageUnits = listOf("片", "袋", "ml")
    var showDosageUnitMenu by remember { mutableStateOf(false) }
    
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
                    timesPerDay = it.timesPerDay.toString()
                    dosageAmount = it.dosageAmount.toString()
                    dosageUnit = it.dosageUnit
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
            
            // 每天服用次数
            OutlinedTextField(
                value = timesPerDay,
                onValueChange = { 
                    if (it.isEmpty() || it.toIntOrNull() != null) {
                        timesPerDay = it
                    }
                },
                label = { Text("每天服用次数") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 用药剂量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dosageAmount,
                    onValueChange = { 
                        if (it.isEmpty() || it.toFloatOrNull() != null) {
                            dosageAmount = it
                        }
                    },
                    label = { Text("每次用量") },
                    modifier = Modifier.weight(1f)
                )
                
                Box {
                    OutlinedButton(
                        onClick = { showDosageUnitMenu = true }
                    ) {
                        Text(dosageUnit)
                    }
                    
                    DropdownMenu(
                        expanded = showDosageUnitMenu,
                        onDismissRequest = { showDosageUnitMenu = false }
                    ) {
                        dosageUnits.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit) },
                                onClick = {
                                    dosageUnit = unit
                                    showDosageUnitMenu = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("服药说明") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    if (patientName.isBlank()) {
                        error = "请输入患者姓名"
                        return@Button
                    }
                    if (medicineName.isBlank()) {
                        error = "请输入药品名称"
                        return@Button
                    }
                    val timesPerDayInt = timesPerDay.toIntOrNull()
                    if (timesPerDayInt == null || timesPerDayInt <= 0) {
                        error = "请输入有效的每天服用次数"
                        return@Button
                    }
                    val dosageAmountFloat = dosageAmount.toFloatOrNull()
                    if (dosageAmountFloat == null || dosageAmountFloat <= 0) {
                        error = "请输入有效的用药剂量"
                        return@Button
                    }
                    
                    // 计算服药间隔（小时）
                    val intervalHours = 24 / timesPerDayInt
                    
                    scope.launch {
                        try {
                            val reminder = MedicationReminder(
                                id = reminderId ?: 0,
                                patientName = patientName,
                                medicineName = medicineName,
                                startDate = startDate,
                                endDate = endDate,
                                firstDoseTime = firstDoseTime,
                                intervalHours = intervalHours,
                                timesPerDay = timesPerDayInt,
                                dosageAmount = dosageAmountFloat,
                                dosageUnit = dosageUnit,
                                instructions = instructions
                            )
                            
                            database.medicationReminderDao().insertOrUpdate(reminder)
                            scheduleReminder(workManager, reminder)
                            navController.popBackStack()
                        } catch (e: Exception) {
                            error = e.message
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (reminderId == null) "添加" else "保存")
            }
        }
    }
}

private fun scheduleReminder(workManager: WorkManager, reminder: MedicationReminder) {
    // 取消该提醒的所有现有工作
    workManager.cancelAllWorkByTag("reminder_${reminder.id}")
    
    val now = LocalDateTime.now()
    if (reminder.endDate.isBefore(now)) return
    
    // 设置提醒数据
    val data = workDataOf(
        "patientName" to reminder.patientName,
        "medicineName" to reminder.medicineName,
        "dosage" to "${reminder.dosageAmount}${reminder.dosageUnit}"
    )
    
    // 计算第一次提醒的延迟时间
    val firstDoseDelay = Duration.between(now, reminder.firstDoseTime)
    if (!firstDoseDelay.isNegative) {
        // 如果第一次服药时间还没到，创建一次性提醒
        val firstDoseRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(firstDoseDelay.toMinutes(), TimeUnit.MINUTES)
            .setInputData(data)
            .addTag("reminder_${reminder.id}")
            .build()
            
        workManager.enqueue(firstDoseRequest)
    }
    
    // 创建周期性提醒
    val periodicRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
        reminder.intervalHours.toLong(),
        TimeUnit.HOURS
    )
        .setInputData(data)
        .addTag("reminder_${reminder.id}")
        // 如果第一次服药时间还没到，设置周期性提醒的开始时间为第一次服药时间
        .apply {
            if (!firstDoseDelay.isNegative) {
                setInitialDelay(firstDoseDelay.toMinutes(), TimeUnit.MINUTES)
            }
        }
        .build()
    
    workManager.enqueue(periodicRequest)
}
