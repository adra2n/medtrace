package com.yy.medtrace.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yy.medtrace.MainActivity
import com.yy.medtrace.R
import com.yy.medtrace.data.AppDatabase
import java.time.LocalDate
import java.time.LocalTime

object ReminderHelper {
    private const val CHANNEL_ID = "health_todo_reminder"
    private const val NOTIFICATION_ID = 1001
    private const val ACTION_DAILY = "com.yy.medtrace.reminder.DAILY"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "健康待办提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "每日健康待办的汇总提醒"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleDaily(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            return
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DAILY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val now = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                now.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                now.timeInMillis,
                pendingIntent
            )
        }
    }

    suspend fun maybeNotify(context: Context, database: AppDatabase) {
        val today = LocalDate.now()
        val todayStr = today.toString()
        // 取当天未完成且今天尚未通知过的待办（基于数据库字段，重装/清数据随备份恢复）
        val pending = database.healthTodoDao().getPendingByDate(today)
            .filter { it.notifiedDate != todayStr }
        if (pending.isEmpty()) return
        // 按内容关键词区分类型，用于通知文案
        val medCount = pending.count { it.content.contains(Regex("服药|用药|吃|药")) }
        val checkupCount = pending.count { it.content.contains(Regex("复查|体检|检查|复诊")) }
        showNotification(context, pending.size, medCount, checkupCount)
        database.healthTodoDao().markNotified(pending.map { it.id }, todayStr)
    }

    fun showNotification(
        context: Context,
        count: Int,
        medCount: Int = 0,
        checkupCount: Int = 0
    ) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val detail = buildList {
            if (medCount > 0) add("服药 $medCount 条")
            if (checkupCount > 0) add("复查 $checkupCount 条")
            if (medCount == 0 && checkupCount == 0) add("待办 $count 条")
        }.joinToString("、")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今日健康待办")
            .setContentText("今天有 $detail，别让健康溜走")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
