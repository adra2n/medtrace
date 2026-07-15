package com.yy.chiyaole.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1, // 只需要一条记录
    val enableNotificationSound: Boolean = true,         // 启用通知声音
    val enableVibration: Boolean = true,                 // 启用震动
    val darkMode: Boolean = false                        // 深色模式
)
