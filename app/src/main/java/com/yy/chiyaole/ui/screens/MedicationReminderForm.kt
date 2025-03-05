package com.yy.chiyaole.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationReminderForm(
    reminder: MedicationReminder? = null,
    onSave: (MedicationReminder) -> Unit,
    onCancel: () -> Unit
) {
    var patientName by remember { mutableStateOf(reminder?.patientName ?: "") }
    var medicineName by remember { mutableStateOf(reminder?.medicineName ?: "") }
    var frequency by remember { mutableStateOf(reminder?.frequency ?: "") }
    var dosage by remember { mutableStateOf(reminder?.dosage ?: "") }
    var instructions by remember { mutableStateOf(reminder?.instructions ?: "") }
    var intervalHours by remember { mutableStateOf(reminder?.intervalHours?.toString() ?: "8") }
    
    var startDate by remember { 
        mutableStateOf(reminder?.startDate?.toLocalDate() ?: LocalDate.now())
    }
    var endDate by remember { 
        mutableStateOf(reminder?.endDate?.toLocalDate() ?: LocalDate.now().plusDays(7))
    }
    var firstDoseTime by remember { 
        mutableStateOf(reminder?.firstDoseTime?.toLocalTime() ?: LocalTime.of(8, 0))
    }
    
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
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
        
        // 第一次服药时间选择
        OutlinedButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        firstDoseTime = LocalTime.of(hourOfDay, minute)
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
        FormTextField(
            value = intervalHours,
            onValueChange = { 
                if (it.isEmpty() || it.toIntOrNull() != null) {
                    intervalHours = it
                }
            },
            label = "服药间隔（小时）"
        )
        
        FormTextField(
            value = frequency,
            onValueChange = { frequency = it },
            label = "服药频率描述（如：每天三次）"
        )
        
        FormTextField(
            value = dosage,
            onValueChange = { dosage = it },
            label = "用药剂量（如：每次一片）"
        )
        
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
                    if (patientName.isBlank() || medicineName.isBlank() || 
                        frequency.isBlank() || dosage.isBlank() || intervalHours.isBlank()
                    ) {
                        error = "请填写必要信息"
                        return@Button
                    }
                    
                    val intervalHoursInt = intervalHours.toIntOrNull()
                    if (intervalHoursInt == null || intervalHoursInt <= 0) {
                        error = "请输入有效的服药间隔时间"
                        return@Button
                    }
                    
                    val newReminder = MedicationReminder(
                        id = reminder?.id ?: 0,
                        patientName = patientName,
                        medicineName = medicineName,
                        startDate = LocalDateTime.of(startDate, LocalTime.MIN),
                        endDate = LocalDateTime.of(endDate, LocalTime.MAX),
                        firstDoseTime = LocalDateTime.of(startDate, firstDoseTime),
                        intervalHours = intervalHoursInt,
                        frequency = frequency,
                        dosage = dosage,
                        instructions = instructions
                    )
                    onSave(newReminder)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("保存")
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
