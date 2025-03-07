package com.yy.chiyaole.util

import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.OneTimeWorkRequestBuilder
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.worker.MedicationReminderWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

fun scheduleMedicationReminder(workManager: WorkManager, reminder: MedicationReminder) {
    // 取消该提醒的所有现有工作
    workManager.cancelAllWorkByTag("reminder_${reminder.id}")
    
    val now = LocalDateTime.now()
    if (reminder.endDate.isBefore(now) || !reminder.isActive) return
    
    // 设置提醒数据
    val data = workDataOf(
        "reminderId" to reminder.id,
        "patientName" to reminder.patientName,
        "medicineName" to reminder.medicineName,
        "dosage" to "${reminder.dosageAmount}${reminder.dosageUnit}"
    )
    
    // 获取今天的服药时间点
    val todayTimes = reminder.medicationTimes.map { time ->
        LocalDateTime.of(now.toLocalDate(), time)
    }
    
    // 为每个今天未过期的时间点设置提醒
    todayTimes.forEach { dateTime ->
        val delay = Duration.between(now, dateTime)
        if (!delay.isNegative) {
            val request = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
                .setInputData(data)
                .addTag("reminder_${reminder.id}")
                .build()
                
            workManager.enqueue(request)
        }
    }
    
    // 为明天的第一个时间点设置提醒
    val tomorrow = now.plusDays(1).toLocalDate()
    if (!tomorrow.isAfter(reminder.endDate.toLocalDate())) {
        val tomorrowFirstTime = LocalDateTime.of(tomorrow, reminder.medicationTimes.first())
        val delayToTomorrow = Duration.between(now, tomorrowFirstTime)
        
        val tomorrowRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delayToTomorrow.toMinutes(), TimeUnit.MINUTES)
            .setInputData(data)
            .addTag("reminder_${reminder.id}")
            .build()
            
        workManager.enqueue(tomorrowRequest)
    }
}
