package com.yy.chiyaole.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.ui.components.FormTextField
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationReminderForm(
    reminder: MedicationReminder? = null,
    onSave: (MedicationReminder) -> Unit,
    onCancel: () -> Unit
) {
    var patientName by remember { mutableStateOf(reminder?.patientName ?: "") }
    var medicineName by remember { mutableStateOf(reminder?.medicineName ?: "") }
    var timesPerDay by remember { mutableStateOf(reminder?.timesPerDay?.toString() ?: "1") }
    var dosageAmount by remember { mutableStateOf(reminder?.dosageAmount?.toString() ?: "1") }
    var dosageUnit by remember { mutableStateOf(reminder?.dosageUnit ?: "片") }
    var instructions by remember { mutableStateOf(reminder?.instructions ?: "") }
    
    var startDate by remember { 
        mutableStateOf(reminder?.startDate?.toLocalDate() ?: LocalDate.now())
    }
    var endDate by remember { 
        mutableStateOf(reminder?.endDate?.toLocalDate() ?: LocalDate.now().plusDays(7))
    }
    
    var medicationTimes by remember {
        mutableStateOf(reminder?.medicationTimes ?: listOf(LocalTime.of(8, 0)))
    }
    
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dosageUnits = listOf("片", "袋", "ml")
    var showDosageUnitMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        FormTextField(
            value = patientName,
            onValueChange = { patientName = it },
            label = "患者姓名"
        )
        
        FormTextField(
            value = medicineName,
            onValueChange = { medicineName = it },
            label = "药品名称"
        )
        
        // 开始日期选择
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        startDate = LocalDate.of(year, month + 1, dayOfMonth)
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
            Text("开始日期：${startDate.format(dateFormatter)}")
        }
        
        // 结束日期选择
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val newEndDate = LocalDate.of(year, month + 1, dayOfMonth)
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
            Text("结束日期：${endDate.format(dateFormatter)}")
        }
        
        // 每天服用次数
        FormTextField(
            value = timesPerDay,
            onValueChange = { newValue -> 
                if (newValue.isEmpty() || newValue.toIntOrNull() != null) {
                    val newTimesPerDay = newValue.toIntOrNull() ?: 1
                    if (newTimesPerDay in 1..4) {
                        timesPerDay = newValue
                        // 根据服用次数调整时间点列表
                        medicationTimes = when (newTimesPerDay) {
                            1 -> listOf(LocalTime.of(8, 0))
                            2 -> listOf(LocalTime.of(8, 0), LocalTime.of(20, 0))
                            3 -> listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))
                            4 -> listOf(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0), LocalTime.of(20, 0))
                            else -> medicationTimes
                        }
                    }
                }
            },
            label = "每天服用次数（1-4次）"
        )
        
        // 服药时间点选择
        Text(
            text = "服药时间",
            style = MaterialTheme.typography.bodyLarge
        )
        medicationTimes.forEachIndexed { index, time ->
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val newTime = LocalTime.of(hourOfDay, minute)
                            medicationTimes = medicationTimes.toMutableList().also {
                                it[index] = newTime
                            }.sorted()
                        },
                        time.hour,
                        time.minute,
                        true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("第${index + 1}次：${time.format(timeFormatter)}")
            }
        }
        
        // 用药剂量
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FormTextField(
                value = dosageAmount,
                onValueChange = { 
                    if (it.isEmpty() || it.toFloatOrNull() != null) {
                        dosageAmount = it
                    }
                },
                label = "每次用量",
//                modifier = Modifier.weight(1f)
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
        
        FormTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = "服药说明（可选）",
            singleLine = false,
            maxLines = 3
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                    if (timesPerDayInt == null || timesPerDayInt !in 1..4) {
                        error = "请输入有效的每天服用次数（1-4次）"
                        return@Button
                    }
                    val dosageAmountFloat = dosageAmount.toFloatOrNull()
                    if (dosageAmountFloat == null || dosageAmountFloat <= 0) {
                        error = "请输入有效的用药剂量"
                        return@Button
                    }
                    
                    val newReminder = MedicationReminder(
                        id = reminder?.id ?: 0,
                        patientName = patientName,
                        medicineName = medicineName,
                        startDate = LocalDateTime.of(startDate, LocalTime.MIN),
                        endDate = LocalDateTime.of(endDate, LocalTime.MAX),
                        timesPerDay = timesPerDayInt,
                        medicationTimes = medicationTimes.sorted(),
                        dosageAmount = dosageAmountFloat,
                        dosageUnit = dosageUnit,
                        instructions = instructions,
                        isActive = true
                    )
                    onSave(newReminder)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (reminder == null) "添加" else "保存")
            }
            
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("取消")
            }
        }
    }
}
