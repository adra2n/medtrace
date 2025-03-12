package com.yy.chiyaole.util

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.*
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.worker.MedicationReminderWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val TAG_PREFIX = "reminder_"
//    private const val TAG_DELAYED = "_delayed"

    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleReminder(
        context: Context,
        reminder: MedicationReminder,
        advanceMinutes: Int = 30
    ) {
        val workManager = WorkManager.getInstance(context)


        // 取消该提醒的所有现有工作
        workManager.cancelAllWorkByTag("reminder_${reminder.id}")
//        cancelReminder(context, reminder.id)

        // 如果提醒不活跃或已过期，直接返回
        if (!reminder.isActive || LocalDateTime.now().isAfter(reminder.endDate)) {
            return
        }

        // 获取当前时间
        val now = LocalDateTime.now()

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

//
//        // 为每个服药时间点创建提醒
//        reminder.medicationTimes.forEach { medicationTime ->
//            // 计算今天的服药时间
//            var nextDoseTime = now.toLocalDate().atTime(medicationTime)
//
//            // 如果今天的这个时间点已经过了，设置为明天
//            if (nextDoseTime.isBefore(now)) {
//                nextDoseTime = nextDoseTime.plusDays(1)
//            }
//
//            // 如果超过了结束日期，不创建提醒
//            if (nextDoseTime.isAfter(reminder.endDate)) {
//                return@forEach
//            }
//
//            // 如果在开始日期之前，使用开始日期
//            if (nextDoseTime.isBefore(reminder.startDate)) {
//                nextDoseTime = reminder.startDate.with(medicationTime)
//            }
//
//            // 计算提前提醒时间
//            val reminderTime = nextDoseTime.minusMinutes(advanceMinutes.toLong())
//
//            // 如果提醒时间已经过了，跳过这次提醒
//            if (reminderTime.isBefore(now)) {
//                return@forEach
//            }
//
//            scheduleReminderWork(
//                context = context,
//                reminder = reminder,
//                nextDoseTime = nextDoseTime,
//                delay = Duration.between(now, reminderTime),
////                isDelayed = false
//            )
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun scheduleReminderWork(
//        context: Context,
//        reminder: MedicationReminder,
//        nextDoseTime: LocalDateTime,
//        delay: Duration,
////        isDelayed: Boolean
//    ) {
//        val workManager = WorkManager.getInstance(context)
//
//        // 创建工作数据
//        val data = workDataOf(
//            "reminderId" to reminder.id,
//            "patientName" to reminder.patientName,
//            "medicineName" to reminder.medicineName,
//            "dosageAmount" to reminder.dosageAmount,
//            "dosageUnit" to reminder.dosageUnit,
//            "scheduledTime" to nextDoseTime.toString(),
//            "isPreview" to false
//        )
//
//        // 创建工作请求
//        val reminderRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
//            .setInputData(data)
//            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
//            .addTag(TAG_PREFIX + reminder.id)
//            .apply {
//
//            }
//            .build()
//
//        // 提交工作请求
//        workManager.enqueue(reminderRequest)
//
//        // 如果不是延迟提醒且不是一次性提醒，创建周期性工作
////        if (!isDelayed && reminder.endDate.isAfter(nextDoseTime.plusDays(1))) {
////            val periodicRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
////                24, TimeUnit.HOURS,
////                15, TimeUnit.MINUTES // 灵活间隔
////            )
////                .setInputData(data)
////                .addTag(TAG_PREFIX + reminder.id)
////                .build()
////
////            workManager.enqueue(periodicRequest)
////        }
//    }
//
//    fun cancelReminder(context: Context, reminderId: Long) {
//        val workManager = WorkManager.getInstance(context)
//        workManager.cancelAllWorkByTag(TAG_PREFIX + reminderId)
////        workManager.cancelAllWorkByTag(TAG_PREFIX + reminderId + TAG_DELAYED)
//    }
}
