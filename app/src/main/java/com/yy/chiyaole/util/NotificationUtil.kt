package com.yy.chiyaole.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yy.chiyaole.MainActivity
import com.yy.chiyaole.R
import com.yy.chiyaole.data.model.MedicationReminder

object NotificationUtil {
    const val CHANNEL_ID = "medication_reminder"
    const val NOTIFICATION_ID = 1
    const val ACTION_TAKEN = "com.yy.chiyaole.ACTION_TAKEN"
    const val ACTION_SKIP = "com.yy.chiyaole.ACTION_SKIP"
    const val EXTRA_REMINDER_ID = "reminder_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "用药提醒"
            val descriptionText = "提醒您按时服药"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showMedicationReminder(
        context: Context,
        reminder: MedicationReminder,
        enableSound: Boolean,
        enableVibration: Boolean
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        // 创建点击通知时的 Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建"已服用"按钮的 Intent
        val takenIntent = Intent(ACTION_TAKEN).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context, 1, takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建"跳过"按钮的 Intent
        val skipIntent = Intent(ACTION_SKIP).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context, 2, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("该吃药啦")
            .setContentText("${reminder.medicineName} ${reminder.dosageAmount}${reminder.dosageUnit}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_check, "已服用", takenPendingIntent)
            .addAction(R.drawable.ic_skip, "跳过", skipPendingIntent)

        // 根据设置配置通知
        if (!enableSound) {
            builder.setSound(null)
        }
        if (!enableVibration) {
            builder.setVibrate(null)
        }

        // 发送通知
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun cancelNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID)
        }
    }
}
