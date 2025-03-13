package com.yy.chiyaole.ui.components

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationRecord
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.data.model.MedicationStatus
import com.yy.chiyaole.util.getNextDoseTime
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReminderCard(
    reminder: MedicationReminder,
    timeFormatter: DateTimeFormatter,
    now: LocalDateTime,
    database: AppDatabase,
    modifier: Modifier = Modifier
) {
    var todayRecords by remember { mutableStateOf<List<MedicationRecord>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // 获取今日用药记录
    LaunchedEffect(reminder.id, now) {
        val startOfDay = now.toLocalDate().atStartOfDay()
        val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay()
        
        Log.e("gaohe_debug", "Fetching records for reminderId: ${reminder.id}, startOfDay: $startOfDay, endOfDay: $endOfDay")

        database.medicationRecordDao().getReminderDayRecords(
            reminderId = reminder.id,
            startOfDay = startOfDay,
            endOfDay = endOfDay
        ).collect { records ->
            Log.e("gaohe_debug", "Fetched records for reminderId: ${reminder.id}")
            Log.e("gaohe_debug", "Fetched records: $records")
            records.forEach { record ->
                Log.e("gaohe_debug", "获取数据: $record")
            }
            todayRecords = records
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = reminder.patientName,
                // 字体居中设置
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Thin,
                fontSize = 25.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = reminder.medicineName,
                        style = MaterialTheme.typography.bodyMedium,
                        // 颜色为红色
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Thin,
                        fontSize = 20.sp
                    )
                    // Spacer(modifier = Modifier.width(30.dp))
                    Spacer(modifier = Modifier.width(70.dp))

                    Text(
                        text = "服用剂量：${reminder.dosageAmount}${reminder.dosageUnit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 20.sp

                    )
                    Spacer(modifier = Modifier.width(70.dp))
                    getNextDoseTime(reminder)?.let { nextDoseTime ->
//                    val dateTimeFormatter=DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")
                        Text(
                            // 时间展示为年月日
                            text = "下次：${nextDoseTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
//                    Log.e("gaohe_debug", "今日用药记录")
//                    // 显示今日用药记录
//                    Log.e("gaohe_debug", todayRecords.toString())
            if (todayRecords.isNotEmpty()) {
                Text(
                    text = "今日服药记录",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                todayRecords.forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "计划时间：${record.scheduledTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = when (record.status) {
                                MedicationStatus.TAKEN -> "已服用"
                                MedicationStatus.SKIPPED -> "已跳过"
                                MedicationStatus.PENDING -> "待服用"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (record.status) {
                                MedicationStatus.TAKEN -> MaterialTheme.colorScheme.primary
                                MedicationStatus.SKIPPED -> MaterialTheme.colorScheme.error
                                MedicationStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    record.actualTime?.let { actualTime ->
                        Text(
                            text = "实际服药时间：${actualTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 添加快速服药按钮
                if (reminder.isActive && now.isAfter(reminder.endDate)) {
                    //                if (reminder.isActive) {
                    val pendingRecords =
                        todayRecords.filter { it.status == MedicationStatus.PENDING }
                    if (pendingRecords.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val record = pendingRecords.first()
                                        val updatedRecord = record.copy(
                                            status = MedicationStatus.TAKEN,
                                            actualTime = LocalDateTime.now()
                                        )
                                        database.medicationRecordDao().update(updatedRecord)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("已服用")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val record = pendingRecords.first()
                                        val updatedRecord = record.copy(
                                            status = MedicationStatus.SKIPPED,
                                            actualTime = LocalDateTime.now()
                                        )
                                        database.medicationRecordDao().update(updatedRecord)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("跳过")
                            }
                        }
                    }
                }
            }

        }
    }
}
@Composable
fun EmptyReminders() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
