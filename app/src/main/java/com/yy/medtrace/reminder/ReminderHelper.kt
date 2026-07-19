package com.yy.medtrace.reminder

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

object ReminderHelper {
    private const val CHANNEL_ID = "health_todo_reminder"
    private const val NOTIFICATION_ID = 1001
    private const val PREFS = "reminder_prefs"
    private const val KEY_LAST_REMINDED_DATE = "last_reminded_date"

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

    private fun alreadyRemindedToday(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getString(KEY_LAST_REMINDED_DATE, "") ?: ""
        return last == LocalDate.now().toString()
    }

    private fun markRemindedToday(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_REMINDED_DATE, LocalDate.now().toString())
            .apply()
    }

    suspend fun maybeNotify(context: Context, database: AppDatabase) {
        if (alreadyRemindedToday(context)) return
        val count = database.healthTodoDao().getPendingCountByDate(LocalDate.now())
        if (count <= 0) {
            markRemindedToday(context)
            return
        }
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今日健康待办")
            .setContentText("今天有 $count 条健康待办，别让健康溜走")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
        markRemindedToday(context)
    }
}
