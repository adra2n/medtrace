package com.yy.chiyaole.worker

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationRecord
import com.yy.chiyaole.data.model.MedicationStatus
import com.yy.chiyaole.util.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Locale
import kotlin.coroutines.resume

class MedicationReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private var textToSpeech: TextToSpeech? = null

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
                val message = "亲爱的${patientName}，现在该吃${medicineName}了，请服用${dosageAmount}${dosageUnit}"
                try {
                    speakMessage(message)
                } catch (e: Exception) {
                    Log.e(TAG, "语音提醒失败", e)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "提醒失败", e)
            Result.failure()
        } finally {
            // 清理 TextToSpeech 资源
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }

    private suspend fun speakMessage(message: String) = suspendCancellableCoroutine { continuation ->
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // 设置语言
                    var result = textToSpeech?.setLanguage(Locale.CHINESE)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        result = textToSpeech?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e(TAG, "语言不支持")
                            continuation.resume(Unit)
                            return@TextToSpeech
                        }
                    }

                    // 设置语音完成的回调
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}

                        override fun onDone(utteranceId: String?) {
                            if (utteranceId == "medication_reminder") {
                                continuation.resume(Unit)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (utteranceId == "medication_reminder") {
                                Log.e(TAG, "语音播放失败")
                                continuation.resume(Unit)
                            }
                        }
                    })

                    // 播放语音
                    textToSpeech?.speak(
                        message,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "medication_reminder"
                    )
                } else {
                    Log.e(TAG, "TextToSpeech 初始化失败: $status")
                    continuation.resume(Unit)
                }
            }

            continuation.invokeOnCancellation {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "语音提醒设置失败", e)
            continuation.resume(Unit)
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
