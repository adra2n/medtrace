package com.yy.medtrace.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yy.medtrace.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != "com.yy.medtrace.reminder.DAILY") return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                runCatching { ReminderHelper.maybeNotify(context, database) }
                ReminderHelper.scheduleDaily(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
