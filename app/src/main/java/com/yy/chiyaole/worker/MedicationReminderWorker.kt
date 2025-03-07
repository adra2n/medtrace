package com.yy.chiyaole.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.yy.chiyaole.R
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationRecord
import com.yy.chiyaole.data.model.MedicationStatus
import com.yy.chiyaole.receiver.MedicationActionReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Locale

class MedicationReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val reminderId = inputData.getLong("reminderId", -1)
            if (reminderId == -1L) return@withContext Result.failure()
            
            val patientName = inputData.getString("patientName") ?: return@withContext Result.failure()
            val medicineName = inputData.getString("medicineName") ?: return@withContext Result.failure()
            val dosage = inputData.getString("dosage") ?: return@withContext Result.failure()
            val scheduledTime = LocalDateTime.now()
            val isPreview = inputData.getBoolean("isPreview", false)

            // 获取用户设置
            val settings = AppDatabase.getDatabase(context).userSettingsDao().getUserSettings().first() ?: return@withContext Result.failure()

            // 如果不是预览，创建服药记录
            if (!isPreview) {
                val record = MedicationRecord(
                    reminderId = reminderId,
                    scheduledTime = scheduledTime,
                    actualTime = null,
                    status = MedicationStatus.PENDING
                )
                AppDatabase.getDatabase(context).medicationRecordDao().insert(record)
            }

            // 构建提醒消息
            val title = if (isPreview) "提醒测试" else "服药提醒"
            val message = if (isPreview) {
                "这是语音提醒测试"
            } else {
                "${patientName}该吃${medicineName}了，请服用${dosage}"
            }

            // 创建通知渠道
            createNotificationChannel()

            // 创建操作按钮的 PendingIntent
            val takenIntent = PendingIntent.getBroadcast(
                context,
                reminderId.toInt() * 10 + 1,
                Intent(context, MedicationActionReceiver::class.java).apply {
                    action = MedicationActionReceiver.ACTION_TAKEN
                    putExtra("reminderId", reminderId)
                    putExtra("scheduledTime", scheduledTime.toString())
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val skipIntent = PendingIntent.getBroadcast(
                context,
                reminderId.toInt() * 10 + 2,
                Intent(context, MedicationActionReceiver::class.java).apply {
                    action = MedicationActionReceiver.ACTION_SKIP
                    putExtra("reminderId", reminderId)
                    putExtra("scheduledTime", scheduledTime.toString())
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 发送通知
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_medicine)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_check, "已服用", takenIntent)
                .addAction(R.drawable.ic_skip, "跳过", skipIntent)

            // 根据设置添加声音
            if (settings.enableNotificationSound || isPreview) {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            }

            // 根据设置添加震动
            if (settings.enableVibration || isPreview) {
                builder.setVibrate(longArrayOf(0, 500, 200, 500))
            }

            // 显示通知
            notificationManager.notify(
                if (isPreview) PREVIEW_NOTIFICATION_ID else reminderId.toInt(),
                builder.build()
            )

            // 语音提醒
            if (settings.enableVoiceReminder || isPreview) {
                withContext(Dispatchers.Main) {
                    initTextToSpeech(message)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("MedicationReminderWorker", "提醒失败", e)
            Result.failure()
        }
    }

    private fun initTextToSpeech(message: String) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(Locale.CHINESE)
                    when (result) {
                        TextToSpeech.LANG_MISSING_DATA,
                        TextToSpeech.LANG_NOT_SUPPORTED -> {
                            textToSpeech?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                        }
                    }
                    ttsInitialized = true
                    speakMessage(message)
                }
            }
        } else {
            speakMessage(message)
        }
    }

    private fun speakMessage(message: String) {
        if (ttsInitialized) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "medication_reminder")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "服药提醒"
            val descriptionText = "用于发送服药提醒通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            ONGOING_NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_medicine)
                .setContentTitle("服药提醒")
                .setContentText("正在运行服药提醒服务")
                .build()
        )
    }

    companion object {
        private const val CHANNEL_ID = "medication_reminder_channel"
        private const val ONGOING_NOTIFICATION_ID = 1
        private const val PREVIEW_NOTIFICATION_ID = 9999
    }
}
