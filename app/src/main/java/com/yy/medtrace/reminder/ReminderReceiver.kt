package com.yy.medtrace.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yy.medtrace.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != "com.yy.medtrace.reminder.DAILY") return
        val database = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { ReminderHelper.maybeNotify(context, database) }
        }
        // 重新排程次日，确保每天触发一次
        ReminderHelper.scheduleDaily(context)
    }
}
