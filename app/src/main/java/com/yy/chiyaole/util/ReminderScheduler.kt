package com.yy.chiyaole.util

import android.content.Context
import androidx.work.*
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.worker.MedicationReminderWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val TAG_PREFIX = "reminder_"
    private const val TAG_DELAYED = "_delayed"

    fun scheduleReminder(
        context: Context,
        reminder: MedicationReminder,
        advanceMinutes: Int = 30
    ) {
        val workManager = WorkManager.getInstance(context)
        
        // 取消该提醒的所有现有工作
        cancelReminder(context, reminder.id)
        
        // 如果提醒不活跃或已过期，直接返回
        if (!reminder.isActive || LocalDateTime.now().isAfter(reminder.endDate)) {
            return
        }
        
        // 获取当前时间
        val now = LocalDateTime.now()
        
        // 为每个服药时间点创建提醒
        reminder.medicationTimes.forEach { medicationTime ->
            // 计算今天的服药时间
            var nextDoseTime = now.toLocalDate().atTime(medicationTime)
            
            // 如果今天的这个时间点已经过了，设置为明天
            if (nextDoseTime.isBefore(now)) {
                nextDoseTime = nextDoseTime.plusDays(1)
            }
            
            // 如果超过了结束日期，不创建提醒
            if (nextDoseTime.isAfter(reminder.endDate)) {
                return@forEach
            }
            
            // 如果在开始日期之前，使用开始日期
            if (nextDoseTime.isBefore(reminder.startDate)) {
                nextDoseTime = reminder.startDate.with(medicationTime)
            }
            
            // 计算提前提醒时间
            val reminderTime = nextDoseTime.minusMinutes(advanceMinutes.toLong())
            
            // 如果提醒时间已经过了，跳过这次提醒
            if (reminderTime.isBefore(now)) {
                return@forEach
            }
            
            scheduleReminderWork(
                context = context,
                reminder = reminder,
                nextDoseTime = nextDoseTime,
                delay = Duration.between(now, reminderTime),
                isDelayed = false
            )
        }
    }

    fun scheduleDelayedReminder(
        context: Context,
        reminder: MedicationReminder,
        delayMinutes: Int
    ) {
        val workManager = WorkManager.getInstance(context)
        val now = LocalDateTime.now()
        val delayedTime = now.plusMinutes(delayMinutes.toLong())
        
        // 取消该提醒的延迟提醒（如果有）
        workManager.cancelAllWorkByTag("${TAG_PREFIX}${reminder.id}${TAG_DELAYED}")
        
        scheduleReminderWork(
            context = context,
            reminder = reminder,
            nextDoseTime = delayedTime,
            delay = Duration.ofMinutes(delayMinutes.toLong()),
            isDelayed = true
        )
    }

    private fun scheduleReminderWork(
        context: Context,
        reminder: MedicationReminder,
        nextDoseTime: LocalDateTime,
        delay: Duration,
        isDelayed: Boolean
    ) {
        val workManager = WorkManager.getInstance(context)
        
        // 创建工作数据
        val data = workDataOf(
            "reminderId" to reminder.id,
            "patientName" to reminder.patientName,
            "medicineName" to reminder.medicineName,
            "dosageAmount" to reminder.dosageAmount,
            "dosageUnit" to reminder.dosageUnit,
            "scheduledTime" to nextDoseTime.toString(),
            "isPreview" to false
        )
        
        // 创建工作请求
        val reminderRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInputData(data)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .addTag(TAG_PREFIX + reminder.id)
            .apply {
                if (isDelayed) {
                    addTag(TAG_PREFIX + reminder.id + TAG_DELAYED)
                }
            }
            .build()
        
        // 提交工作请求
        workManager.enqueue(reminderRequest)
        
        // 如果不是延迟提醒且不是一次性提醒，创建周期性工作
        if (!isDelayed && reminder.endDate.isAfter(nextDoseTime.plusDays(1))) {
            val periodicRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // 灵活间隔
            )
                .setInputData(data)
                .addTag(TAG_PREFIX + reminder.id)
                .build()
            
            workManager.enqueue(periodicRequest)
        }
    }
    
    fun cancelReminder(context: Context, reminderId: Long) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG_PREFIX + reminderId)
        workManager.cancelAllWorkByTag(TAG_PREFIX + reminderId + TAG_DELAYED)
    }
}
