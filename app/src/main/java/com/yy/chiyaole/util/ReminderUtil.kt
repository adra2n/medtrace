package com.yy.chiyaole.util

import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.worker.MedicationReminderWorker
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

fun scheduleMedicationReminder(workManager: WorkManager, reminder: MedicationReminder) {
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
