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
        val action = intent?.action
        if (action != "com.yy.medtrace.reminder.DAILY" && 
            action != "com.yy.medtrace.reminder.TODO") return
        
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                
                if (action == "com.yy.medtrace.reminder.DAILY") {
                    // 每日汇总通知
                    runCatching { ReminderHelper.maybeNotify(context, database) }
                    ReminderHelper.scheduleDaily(context)
                } else if (action == "com.yy.medtrace.reminder.TODO") {
                    // 精确时间点的待办提醒
                    val todoId = intent.getLongExtra("todo_id", -1)
                    val todoContent = intent.getStringExtra("todo_content") ?: ""
                    val todoTime = intent.getStringExtra("todo_time") ?: ""
                    
                    if (todoId != -1L && todoContent.isNotBlank()) {
                        // 查询待办详情并显示通知
                        val todo = database.healthTodoDao().getById(todoId)
                        if (todo != null && !todo.done) {
                            ReminderHelper.showTodoNotification(context, todo)
                        }
                    }
                    
                    // 为明天重新设置闹钟（因为是精确一次性闹钟）
                    val today = java.time.LocalDate.now()
                    val todos = database.healthTodoDao().getPendingByDate(today)
                    todos.filter { !it.done && it.reminderTime.isNotBlank() }.forEach { todo ->
                        ReminderHelper.scheduleForTodo(context, todo)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
