package com.yy.chiyaole.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yy.chiyaole.MainActivity
import com.yy.chiyaole.R
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.receiver.MedicationActionReceiver

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

    fun showMedicationReminder(
        context: Context,
        reminder: MedicationReminder,
        enableSound: Boolean = true,
        enableVibration: Boolean = true
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 创建通知渠道
        createNotificationChannel(context)
        
        // 创建操作按钮的 PendingIntent
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takenIntent = createActionPendingIntent(
            context,
            reminder.id,
            ACTION_TAKEN,
            reminder.scheduledTime.toString()
        )

        val skipIntent = createActionPendingIntent(
            context,
            reminder.id,
            ACTION_SKIP,
            reminder.scheduledTime.toString()
        )

        val delayIntent = createActionPendingIntent(
            context,
            reminder.id,
            ACTION_DELAY,
            reminder.scheduledTime.toString()
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
            .addAction(R.drawable.ic_check, "已服用", takenIntent)
            .addAction(R.drawable.ic_skip, "跳过", skipIntent)
            .addAction(R.drawable.ic_delay, "延迟15分钟", delayIntent)
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
    }

    private fun buildReminderMessage(reminder: MedicationReminder): String {
        return "${reminder.patientName}该吃${reminder.medicineName}了，" +
                "请服用${reminder.dosageAmount}${reminder.dosageUnit}" +
                (if (reminder.instructions.isNotBlank()) "，${reminder.instructions}" else "")
    }

    private fun createActionPendingIntent(
        context: Context,
        reminderId: Long,
        action: String,
        scheduledTime: String
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            (reminderId.toInt() * 10) + when (action) {
                ACTION_TAKEN -> 1
                ACTION_SKIP -> 2
                ACTION_DELAY -> 3
                else -> 0
            },
            Intent(context, MedicationActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra("scheduledTime", scheduledTime)
                if (action == ACTION_DELAY) {
                    putExtra(EXTRA_DELAY_MINUTES, 15)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        
        // 检查是否还有其他通知，如果没有，取消摘要通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.activeNotifications.none { it.id != SUMMARY_ID }) {
                notificationManager.cancel(SUMMARY_ID)
            }
        }
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
