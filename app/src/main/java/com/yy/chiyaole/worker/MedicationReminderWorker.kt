package com.yy.chiyaole.worker

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationRecord
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.data.model.MedicationStatus
//import com.yy.chiyaole.data.model.RepeatType
import com.yy.chiyaole.util.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import java.util.concurrent.TimeUnit

class MedicationReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private lateinit var notificationManager: NotificationManager

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 如果不是预览模式，获取用户设置
//            val isPreview = inputData.getBoolean("isPreview", false)
//            val settings = if (!isPreview) {
//                AppDatabase.getDatabase(context).userSettingsDao().getUserSettings().first()
//                    ?: return@withContext Result.failure()
//            } else null

            val reminderId = inputData.getLong("reminderId", -1)
            if (reminderId == -1L) return@withContext Result.failure()
//
            // 获取提醒记录
            val reminder = AppDatabase.getDatabase(context).medicationReminderDao()
                .getById(reminderId) ?: return@withContext Result.failure()
//
            // 检查是否已经服用
            val record = AppDatabase.getDatabase(context).medicationRecordDao()
                .getLatestRecordForReminder(reminderId)
                .first()

            // 如果没有记录，创建一条新记录
            if (record == null) {
                val newRecord = MedicationRecord(
                    reminderId = reminderId,
                    scheduledTime = reminder.scheduledTime,
                    actualTime = null,
                    status = MedicationStatus.PENDING,
                    note = ""
                )
                AppDatabase.getDatabase(context).medicationRecordDao().insert(newRecord)
            }

            // 如果药品还未服用，继续发送提醒
            if (record == null || record.status == MedicationStatus.PENDING) {
                // 获取用户设置
                val settings = AppDatabase.getDatabase(context).userSettingsDao()
                    .getUserSettings()
                    .first() ?: return@withContext Result.failure()
//
//                // 重新发送通知
                NotificationUtil.showMedicationReminder(
                    context = context,
                    reminder = reminder,
                    enableSound = settings.enableNotificationSound,
                    enableVibration = settings.enableVibration,
                    enableVoice = settings.enableVoiceReminder,
                )
//
//                // 调度下一次提醒
                val repeatWorkRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                    .setInitialDelay(30, TimeUnit.SECONDS)  // 30秒后再次提醒
                    .setInputData(
                        workDataOf(
                            "reminderId" to reminderId,
                            "notificationId" to reminderId.toInt()
                        )
                    )
                    .addTag("repeat_reminder_$reminderId")
                    .build()
//
                WorkManager.getInstance(context).enqueue(repeatWorkRequest)
            }

            if (record != null && record.status == MedicationStatus.TAKEN) {
                // 如果已经服用，取消通知
                NotificationUtil.cancelNotification(context, reminderId)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "重复提醒失败", e)
            Result.failure()
        }
    }


//        try {
//            val reminderId = inputData.getLong("reminderId", -1)
//            if (reminderId == -1L) return@withContext Result.failure()
//
//            val patientName = inputData.getString("patientName") ?: return@withContext Result.failure()
//            val medicineName = inputData.getString("medicineName") ?: return@withContext Result.failure()
//            val dosageAmount = inputData.getFloat("dosageAmount", 0f)
//            val dosageUnit = inputData.getString("dosageUnit") ?: return@withContext Result.failure()
//            val scheduledTimeStr = inputData.getString("scheduledTime")
//            val scheduledTime = if (scheduledTimeStr != null) LocalDateTime.parse(scheduledTimeStr) else LocalDateTime.now()
//            val isPreview = inputData.getBoolean("isPreview", false)
//
//            // 如果不是预览模式，获取用户设置
//            val settings = if (!isPreview) {
//                AppDatabase.getDatabase(context).userSettingsDao().getUserSettings().first()
//                    ?: return@withContext Result.failure()
//            } else null

        // 如果不是预览，创建或更新服药记录
//            if (!isPreview) {
//                val existingRecord = AppDatabase.getDatabase(context).medicationRecordDao()
//                    .getRecordsBetween(
//                        scheduledTime.minusMinutes(1),
//                        scheduledTime.plusMinutes(1)
//                    ).first().firstOrNull { it.reminderId == reminderId }
//
//                if (existingRecord == null) {
//                    // 创建新记录
//                    val record = MedicationRecord(
//                        reminderId = reminderId,
//                        scheduledTime = scheduledTime,
//                        actualTime = null,
////                        delayedTime = null,
//                        status = MedicationStatus.PENDING,
//                        note = "",
////                        delayReason = ""
//                    )
//                    AppDatabase.getDatabase(context).medicationRecordDao().insert(record)
//                }
//            }

        // 显示通知
//            if (!isPreview) {
//                val reminder = AppDatabase.getDatabase(context).medicationReminderDao().getById(reminderId)
//                    ?: return@withContext Result.failure()
//                //TODO: review
//                NotificationUtil.showMedicationReminder(
//                    context = context,
//                    reminder = reminder,
//                    enableSound = settings?.enableNotificationSound == true,
//                    enableVibration = settings?.enableVibration == true
//                )
//            }

//            // 语音提醒（预览模式或启用了语音提醒）
//            if (isPreview || settings?.enableVoiceReminder == true) {
//                // 使用 NotificationUtil 来发送提醒
//                NotificationUtil.showMedicationReminder(
//                    context = applicationContext,
//                    reminder = MedicationReminder(
//                        id = 0,
//                        patientName = patientName,
//                        medicineName = medicineName,
//                        dosageAmount = dosageAmount,
//                        dosageUnit = dosageUnit,
//                        instructions = "",
//                        startDate = LocalDateTime.now(),
//                        endDate = LocalDateTime.now(),
//                        timesPerDay = 1,
//                        medicationTimes = listOf(LocalTime.now()),
//                        scheduledTime = LocalDateTime.now()
//                    ),
//                    enableSound = false,  // 禁用声音，因为这里只需要语音提醒
//                    enableVibration = true,  // 启用振动提醒
//                    enableVoice = true  // 启用语音提醒
//                )
//            }
//
//            Result.success()
//        } catch (e: Exception) {
//            Log.e(TAG, "提醒失败", e)
//            Result.failure()
//        }
//    }

//    override suspend fun getForegroundInfo(): ForegroundInfo {
//        return ForegroundInfo(
//            ONGOING_NOTIFICATION_ID,
//            NotificationUtil.createOngoingNotification(context)
//        )
//    }

        companion object {
        private const val TAG = "MedicationReminderWorker"
        private const val ONGOING_NOTIFICATION_ID = 1
    }
}
