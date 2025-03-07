package com.yy.chiyaole.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.yy.chiyaole.MainActivity
import com.yy.chiyaole.R
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.receiver.MedicationActionReceiver
import com.yy.chiyaole.worker.RepeatReminderWorker
import kotlinx.coroutines.*
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object NotificationUtil {
    private const val CHANNEL_ID = "medication_reminder_channel"
    private const val GROUP_KEY = "com.yy.chiyaole.MEDICATION_REMINDERS"
    private const val SUMMARY_ID = 0
    const val PREVIEW_NOTIFICATION_ID = 9999

    const val ACTION_TAKEN = "com.yy.chiyaole.ACTION_MEDICATION_TAKEN"
    const val ACTION_SKIP = "com.yy.chiyaole.ACTION_MEDICATION_SKIP"
    const val ACTION_DELAY = "com.yy.chiyaole.ACTION_MEDICATION_DELAY"

    const val EXTRA_REMINDER_ID = "reminderId"
    const val EXTRA_DELAY_MINUTES = "delayMinutes"
    private const val REPEAT_INTERVAL = 30L // 重复提醒间隔（秒）

    private var textToSpeech: TextToSpeech? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    fun showMedicationReminder(
        context: Context,
        reminder: MedicationReminder,
        enableSound: Boolean = true,
        enableVibration: Boolean = true,
        enableVoice: Boolean = true
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建通知渠道
        createNotificationChannel(context)
        
        // 创建点击通知时的 Intent
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建提醒消息
        val message = buildReminderMessage(reminder)

        // 创建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_medicine)
            .setContentTitle("服药提醒")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)  
            .setOngoing(true)      // 设置为持续通知
            .setContentIntent(contentIntent)
            .setGroup(GROUP_KEY)
            .apply {
                if (enableSound) {
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                }
                if (enableVibration) {
                    setVibrate(longArrayOf(0, 500, 200, 500))
                }
            }
            .build()

        // 发送通知
        notificationManager.notify(reminder.id.toInt(), notification)

        // 更新摘要通知
        updateSummaryNotification(context, notificationManager)

        // 播放语音提醒
        if (enableVoice) {
            coroutineScope.launch {
                try {
                    speakMessage(context, message)
                } catch (e: Exception) {
                    Log.e("NotificationUtil", "语音提醒失败", e)
                }
            }
        }

        // 调度重复提醒
        scheduleRepeatReminder(context, reminder)
    }

    private fun scheduleRepeatReminder(context: Context, reminder: MedicationReminder) {
        val workManager = WorkManager.getInstance(context)
        
        // 创建重复提醒的工作请求
        val repeatWorkRequest = OneTimeWorkRequestBuilder<RepeatReminderWorker>()
            .setInitialDelay(REPEAT_INTERVAL, TimeUnit.SECONDS)
            .setInputData(workDataOf(
                "reminderId" to reminder.id,
                "notificationId" to reminder.id.toInt(),
                "enableVoice" to true  // 添加语音提醒标志
            ))
            .addTag("repeat_reminder_${reminder.id}")
            .build()

        // 取消之前的重复提醒（如果有）
        workManager.cancelAllWorkByTag("repeat_reminder_${reminder.id}")
        
        // 开始新的重复提醒
        workManager.enqueue(repeatWorkRequest)
    }

    private suspend fun speakMessage(context: Context, message: String) = suspendCancellableCoroutine { continuation ->
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // 设置语言
                    var result = textToSpeech?.setLanguage(Locale.CHINESE)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        result = textToSpeech?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("NotificationUtil", "语言不支持")
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
                                // 清理资源
                                textToSpeech?.shutdown()
                                textToSpeech = null
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (utteranceId == "medication_reminder") {
                                Log.e("NotificationUtil", "语音播放失败")
                                continuation.resume(Unit)
                                // 清理资源
                                textToSpeech?.shutdown()
                                textToSpeech = null
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
                    Log.e("NotificationUtil", "TextToSpeech 初始化失败: $status")
                    continuation.resume(Unit)
                    // 清理资源
                    textToSpeech?.shutdown()
                    textToSpeech = null
                }
            }

            continuation.invokeOnCancellation {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                textToSpeech = null
            }
        } catch (e: Exception) {
            Log.e("NotificationUtil", "语音提醒设置失败", e)
            continuation.resume(Unit)
            // 清理资源
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }

    private fun buildReminderMessage(reminder: MedicationReminder): String {
        return "请${reminder.patientName}服用${reminder.medicineName}，" +
                "请服用${reminder.dosageAmount}${reminder.dosageUnit}" +
                (if (reminder.instructions.isNotBlank()) "，${reminder.instructions}" else "")
    }

    private fun updateSummaryNotification(context: Context, notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_medicine)
                .setContentTitle("服药提醒")
                .setContentText("您有多个待服用的药物")
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(SUMMARY_ID, summaryNotification)
        }
    }

    fun createNotificationChannel(context: Context) {
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
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun cancelNotification(context: Context, reminderId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminderId.toInt())
        
        // 取消重复提醒
        WorkManager.getInstance(context).cancelAllWorkByTag("repeat_reminder_$reminderId")
        
        // 检查是否还有其他通知，如果没有，取消摘要通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.activeNotifications.none { it.id != SUMMARY_ID }) {
                notificationManager.cancel(SUMMARY_ID)
            }
        }

        // 清理语音资源
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        coroutineScope.cancel()
    }

    fun createOngoingNotification(context: Context): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_medicine)
            .setContentTitle("服药提醒")
            .setContentText("正在运行服药提醒服务")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
