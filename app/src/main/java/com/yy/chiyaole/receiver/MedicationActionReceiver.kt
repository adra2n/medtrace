package com.yy.chiyaole.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yy.chiyaole.data.AppDatabase
import com.yy.chiyaole.data.model.MedicationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MedicationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminderId", -1)
        val scheduledTimeStr = intent.getStringExtra("scheduledTime") ?: return
        val scheduledTime = LocalDateTime.parse(scheduledTimeStr)
        
        if (reminderId == -1L) return
        
        val database = AppDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            // 查找对应的服药记录
            database.medicationRecordDao().getRecordsBetween(
                scheduledTime.minusMinutes(1),
                scheduledTime.plusMinutes(1)
            ).collect { records ->
                val record = records.firstOrNull { it.reminderId == reminderId } ?: return@collect
                
                // 根据操作更新记录
                val updatedRecord = when (intent.action) {
                    ACTION_TAKEN -> record.copy(
                        status = MedicationStatus.TAKEN,
                        actualTime = LocalDateTime.now()
                    )
                    ACTION_SKIP -> record.copy(
                        status = MedicationStatus.SKIPPED,
                        actualTime = LocalDateTime.now()
                    )
                    else -> return@collect
                }
                
                // 更新记录
                database.medicationRecordDao().update(updatedRecord)
                
                // 取消通知
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(reminderId.toInt())
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "com.yy.chiyaole.ACTION_MEDICATION_TAKEN"
        const val ACTION_SKIP = "com.yy.chiyaole.ACTION_MEDICATION_SKIP"
    }
}
