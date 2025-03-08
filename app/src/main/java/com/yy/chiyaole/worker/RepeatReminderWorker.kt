package com.yy.chiyaole.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationStatus
import com.yy.chiyaole.util.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class RepeatReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val reminderId = inputData.getLong("reminderId", -1)
            if (reminderId == -1L) return@withContext Result.failure()

            // 获取提醒记录
            val reminder = AppDatabase.getDatabase(context).medicationReminderDao()
                .getById(reminderId) ?: return@withContext Result.failure()

            // 检查是否已经服用
            val record = AppDatabase.getDatabase(context).medicationRecordDao()
                .getLatestRecordForReminder(reminderId)
                .first()

            // 如果药品还未服用，继续发送提醒
            if (record == null || record.status == MedicationStatus.PENDING) {
                // 获取用户设置
                val settings = AppDatabase.getDatabase(context).userSettingsDao()
                    .getUserSettings()
                    .first() ?: return@withContext Result.failure()

                // 重新发送通知
                NotificationUtil.showMedicationReminder(
                    context = context,
                    reminder = reminder,
                    enableSound = settings.enableNotificationSound,
                    enableVibration = settings.enableVibration,
                    enableVoice = settings.enableVoiceReminder,
                )

                // 调度下一次提醒
                val repeatWorkRequest = OneTimeWorkRequestBuilder<RepeatReminderWorker>()
                    .setInitialDelay(30, TimeUnit.SECONDS)  // 30秒后再次提醒
                    .setInputData(workDataOf(
                        "reminderId" to reminderId,
                        "notificationId" to reminderId.toInt()
                    ))
                    .addTag("repeat_reminder_$reminderId")
                    .build()

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

    companion object {
        private const val TAG = "RepeatReminderWorker"
    }
}
