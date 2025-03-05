package com.yy.chiyaole.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.ui.components.FormTextField
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordForm(
    record: MedicalRecord? = null,
    onSave: (MedicalRecord) -> Unit,
    onCancel: () -> Unit
) {
    var patientName by remember { mutableStateOf(record?.patientName ?: "") }
    var diagnosis by remember { mutableStateOf(record?.diagnosis ?: "") }
    var medications by remember { mutableStateOf(record?.medications ?: "") }
    var frequency by remember { mutableStateOf(record?.frequency ?: "") }
    var dosage by remember { mutableStateOf(record?.dosage ?: "") }
    var notes by remember { mutableStateOf(record?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (record == null) "新增医疗记录" else "编辑医疗记录") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                value = diagnosis,
                onValueChange = { diagnosis = it },
                label = "诊断结果"
            )
            
            FormTextField(
                value = medications,
                onValueChange = { medications = it },
                label = "开具药品"
            )
            
            FormTextField(
                value = frequency,
                onValueChange = { frequency = it },
                label = "服药频率（如：每天三次）"
            )
            
            FormTextField(
                value = dosage,
                onValueChange = { dosage = it },
                label = "用药剂量（如：每次一片）"
            )
            
            FormTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "备注（可选）"
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
                        
                        val newRecord = MedicalRecord(
                            id = record?.id ?: 0,
                            patientName = patientName,
                            diagnosis = diagnosis,
                            onsetTime = LocalDateTime.now(),
                            medications = medications,
                            frequency = frequency,
                            dosage = dosage,
                            notes = notes
                        )
                        onSave(newRecord)
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
}
