package com.yy.chiyaole.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationStatus
import com.yy.chiyaole.util.NotificationUtil
import com.yy.chiyaole.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MedicationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationUtil.EXTRA_REMINDER_ID, -1)
        val scheduledTimeStr = intent.getStringExtra("scheduledTime") ?: return
        val scheduledTime = LocalDateTime.parse(scheduledTimeStr)
        
        if (reminderId == -1L) return
        
        val database = AppDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            when (intent.action) {
//                NotificationUtil.ACTION_DELAY -> {
//                    // 获取延迟时间（分钟）
//                    val delayMinutes = intent.getIntExtra(NotificationUtil.EXTRA_DELAY_MINUTES, 15)
//
//                    // 获取提醒信息
//                    database.medicationReminderDao().getById(reminderId)?.let { reminder ->
//                        // 重新调度提醒
//                        ReminderScheduler.scheduleDelayedReminder(
//                            context = context,
//                            reminder = reminder,
//                            delayMinutes = delayMinutes
//                        )
//
//                        // 更新记录状态为延迟
//                        database.medicationRecordDao().getRecordsBetween(
//                            scheduledTime.minusMinutes(1),
//                            scheduledTime.plusMinutes(1)
//                        ).collect { records ->
//                            val record = records.firstOrNull { it.reminderId == reminderId }
//                            record?.let {
//                                val updatedRecord = it.copy(
//                                    status = MedicationStatus.DELAYED,
//                                    actualTime = null,
//                                    delayedTime = LocalDateTime.now().plusMinutes(delayMinutes.toLong())
//                                )
//                                database.medicationRecordDao().update(updatedRecord)
//                            }
//                        }
//                    }
//
//                    // 取消当前通知
//                    NotificationUtil.cancelNotification(context, reminderId)
//                }
                
                NotificationUtil.ACTION_TAKEN, NotificationUtil.ACTION_SKIP -> {
                    // 查找对应的服药记录
                    database.medicationRecordDao().getRecordsBetween(
                        scheduledTime.minusMinutes(1),
                        scheduledTime.plusMinutes(1)
                    ).collect { records ->
                        val record = records.firstOrNull { it.reminderId == reminderId } ?: return@collect
                        
                        // 根据操作更新记录
                        val updatedRecord = when (intent.action) {
                            NotificationUtil.ACTION_TAKEN -> record.copy(
                                status = MedicationStatus.TAKEN,
                                actualTime = LocalDateTime.now(),
                                delayedTime = null
                            )
                            NotificationUtil.ACTION_SKIP -> record.copy(
                                status = MedicationStatus.SKIPPED,
                                actualTime = LocalDateTime.now(),
                                delayedTime = null
                            )
                            else -> return@collect
                        }
                        
                        // 更新记录
                        database.medicationRecordDao().update(updatedRecord)
                        
                        // 取消通知
                        NotificationUtil.cancelNotification(context, reminderId)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = NotificationUtil.ACTION_TAKEN
        const val ACTION_SKIP = NotificationUtil.ACTION_SKIP
//        const val ACTION_DELAY = NotificationUtil.ACTION_DELAY
    }
}
