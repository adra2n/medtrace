package com.yy.chiyaole.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
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

class MedicationReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private lateinit var notificationManager: NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val reminderId = inputData.getLong("reminderId", -1)
            if (reminderId == -1L) return@withContext Result.failure()

            val patientName = inputData.getString("patientName") ?: return@withContext Result.failure()
            val medicineName = inputData.getString("medicineName") ?: return@withContext Result.failure()
            val dosageAmount = inputData.getFloat("dosageAmount", 0f)
            val dosageUnit = inputData.getString("dosageUnit") ?: return@withContext Result.failure()
            val scheduledTimeStr = inputData.getString("scheduledTime")
            val scheduledTime = if (scheduledTimeStr != null) LocalDateTime.parse(scheduledTimeStr) else LocalDateTime.now()
            val isPreview = inputData.getBoolean("isPreview", false)

            // 如果不是预览模式，获取用户设置
            val settings = if (!isPreview) {
                AppDatabase.getDatabase(context).userSettingsDao().getUserSettings().first()
                    ?: return@withContext Result.failure()
            } else null

            // 如果不是预览，创建或更新服药记录
            if (!isPreview) {
                val existingRecord = AppDatabase.getDatabase(context).medicationRecordDao()
                    .getRecordsBetween(
                        scheduledTime.minusMinutes(1),
                        scheduledTime.plusMinutes(1)
                    ).first().firstOrNull { it.reminderId == reminderId }

                if (existingRecord == null) {
                    // 创建新记录
                    val record = MedicationRecord(
                        reminderId = reminderId,
                        scheduledTime = scheduledTime,
                        actualTime = null,
                        delayedTime = null,
                        status = MedicationStatus.PENDING,
                        note = "",
                        delayReason = ""
                    )
                    AppDatabase.getDatabase(context).medicationRecordDao().insert(record)
                } else if (existingRecord.status == MedicationStatus.DELAYED) {
                    // 如果是延迟的记录，更新状态为待服用
                    val updatedRecord = existingRecord.copy(
                        status = MedicationStatus.PENDING,
                        delayedTime = null,
                        delayReason = "${existingRecord.delayReason}\n延迟提醒时间：${LocalDateTime.now()}"
                    )
                    AppDatabase.getDatabase(context).medicationRecordDao().update(updatedRecord)
                }
            }

            // 显示通知
            if (!isPreview) {
                val reminder = AppDatabase.getDatabase(context).medicationReminderDao().getById(reminderId)
                    ?: return@withContext Result.failure()
                NotificationUtil.showMedicationReminder(
                    context = context,
                    reminder = reminder,
                    enableSound = settings?.enableNotificationSound == true,
                    enableVibration = settings?.enableVibration == true
                )
            }

            // 语音提醒（预览模式或启用了语音提醒）
            if (isPreview || settings?.enableVoiceReminder == true) {
                // 使用 NotificationUtil 来发送提醒
                NotificationUtil.showMedicationReminder(
                    context = applicationContext,
                    reminder = MedicationReminder(
                        id = 0,
                        patientName = patientName,
                        medicineName = medicineName,
                        dosageAmount = dosageAmount,
                        dosageUnit = dosageUnit,
                        instructions = "",
                        startDate = LocalDateTime.now(),
                        endDate = LocalDateTime.now(),
                        timesPerDay = 1,
                        medicationTimes = listOf(LocalTime.now()),
                        scheduledTime = LocalDateTime.now()
                    ),
                    enableSound = false,  // 禁用声音，因为这里只需要语音提醒
                    enableVibration = true,  // 启用振动提醒
                    enableVoice = true  // 启用语音提醒
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "提醒失败", e)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            ONGOING_NOTIFICATION_ID,
            NotificationUtil.createOngoingNotification(context)
        )
    }

    companion object {
        private const val TAG = "MedicationReminderWorker"
        private const val ONGOING_NOTIFICATION_ID = 1
    }
}
