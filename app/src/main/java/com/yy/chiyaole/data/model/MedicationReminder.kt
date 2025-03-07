package com.yy.chiyaole.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "medication_reminders")
@TypeConverters(Converters::class)
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String,
    val medicineName: String,
    val startDate: LocalDateTime,    // 开始服药日期
    val endDate: LocalDateTime,      // 结束服药日期
    val timesPerDay: Int,            // 每天服用次数
    val medicationTimes: List<LocalTime>, // 每天的服药时间点列表
    val dosageAmount: Float,         // 每次服用数量
    val dosageUnit: String,          // 剂量单位（片、袋、ml）
    val instructions: String = "",    // 服药说明
    val isActive: Boolean = true     // 是否启用提醒
)

class Converters {
    @TypeConverter
    fun fromString(value: String): List<LocalTime> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(",").map { LocalTime.parse(it) }
        }
    }

    @TypeConverter
    fun toString(times: List<LocalTime>): String {
        return times.joinToString(",") { it.toString() }
    }
}
