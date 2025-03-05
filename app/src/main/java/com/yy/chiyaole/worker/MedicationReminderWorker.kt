package com.yy.chiyaole.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.yy.chiyaole.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class MedicationReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private var tts: TextToSpeech? = null
    private val channelId = "medication_reminder_channel"
    private val notificationId = 1
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val patientName = inputData.getString("patientName") ?: return@withContext Result.failure()
            val medicineName = inputData.getString("medicineName") ?: return@withContext Result.failure()
            val dosage = inputData.getString("dosage") ?: return@withContext Result.failure()
            
            val message = "${patientName}，该吃${medicineName}了，剂量：${dosage}"
            
            // 创建通知渠道
            createNotificationChannel()
            
            // 发送通知
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification_new)
                .setContentTitle("用药提醒")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(notificationId, notification)
            
            // 语音播报
            speakMessage(patientName, medicineName, dosage)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "用药提醒"
            val descriptionText = "提醒用户按时服药"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                
                // 设置声音
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun speakMessage(patientName: String, medicineName: String, dosage: String) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                val text = "${patientName}，该吃${medicineName}了，剂量是${dosage}"
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "medication_reminder")
            }
        }
    }
    
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_new)
            .setContentTitle("用药提醒服务运行中")
            .setContentText("正在等待下一次提醒时间")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        return ForegroundInfo(notificationId + 1, notification)
    }
}
