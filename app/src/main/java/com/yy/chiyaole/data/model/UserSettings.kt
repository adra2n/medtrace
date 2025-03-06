package com.yy.chiyaole.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1, // 只需要一条记录
    val sleepStartTime: LocalTime = LocalTime.of(22, 0), // 默认晚上10点
    val sleepEndTime: LocalTime = LocalTime.of(6, 0),    // 默认早上6点
    val enableVoiceReminder: Boolean = true,             // 启用语音提醒
    val enableNotificationSound: Boolean = true,         // 启用通知声音
    val enableVibration: Boolean = true,                 // 启用震动
    val reminderAdvanceMinutes: Int = 5,                // 提前提醒时间（分钟）
    val darkMode: Boolean = false                        // 深色模式
)
