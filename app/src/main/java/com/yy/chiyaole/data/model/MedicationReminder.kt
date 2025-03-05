package com.yy.chiyaole.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.Duration

@Entity(tableName = "medication_reminders")
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val medicineName: String,
    val startDate: LocalDateTime,    // 开始服药日期
    val endDate: LocalDateTime,      // 结束服药日期
    val firstDoseTime: LocalDateTime, // 第一次服药时间
    val intervalHours: Int,          // 服药间隔（小时）
    val frequency: String,           // 服药频率描述
    val dosage: String,             // 用药剂量
    val instructions: String = "",   // 服药说明
    val isActive: Boolean = true    // 是否启用提醒
)
