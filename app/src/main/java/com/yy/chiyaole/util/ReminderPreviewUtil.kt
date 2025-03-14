package com.yy.chiyaole.util

import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.RingtoneManager
import android.util.Log
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.OneTimeWorkRequestBuilder
import com.yy.chiyaole.worker.MedicationReminderWorker
import java.time.LocalDateTime

class ReminderPreviewUtil(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val workManager = WorkManager.getInstance(context)

    init {
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun previewNotificationSound() {
        try {
            stopAll()
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, notification)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("ReminderPreviewUtil", "播放通知声音失败", e)
        }
    }

    fun previewVoiceReminder(volume: Int = 60) {
        stopAll()
        workManager.cancelAllWorkByTag("preview_reminder")

        // 使用MedicationReminderWorker来预览语音提醒
        val data = workDataOf(
            "reminderId" to PREVIEW_REMINDER_ID,
            "patientName" to "测试用户",
            "medicineName" to "测试药品",
            "dosageAmount" to 1f,
            "dosageUnit" to "片",
            "scheduledTime" to LocalDateTime.now().toString(),
            "isPreview" to true,
            "volume" to volume
        )

        val previewRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInputData(data)
            .addTag("preview_reminder")
            .build()

        workManager.enqueue(previewRequest)
    }

    fun previewVibration() {
        stopAll()
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            Log.e("ReminderPreviewUtil", "震动失败", e)
        }
    }

    fun stopAll() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                release()
            } catch (e: Exception) {
                Log.e("ReminderPreviewUtil", "停止媒体播放失败", e)
            }
        }
        mediaPlayer = null

        workManager.cancelAllWorkByTag("preview_reminder")
        vibrator?.cancel()
    }

    fun release() {
        stopAll()
        vibrator = null
    }

    companion object {
        private const val PREVIEW_REMINDER_ID = 9999L
    }
}
