package com.yy.chiyaole.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 用药提醒实体类
 *
 * @property id 主键ID
 * @property patientName 患者姓名
 * @property medicineName 药品名称
 * @property startDate 开始服药日期
 * @property endDate 结束服药日期
 * @property timesPerDay 每天服用次数
 * @property medicationTimes 每天的服药时间点列表
 * @property dosageAmount 每次服用数量
 * @property dosageUnit 剂量单位（片、袋、ml等）
 * @property instructions 服药说明（如：饭前/饭后服用）
 * @property isActive 是否启用提醒
 * @property scheduledTime 当前计划服药时间（用于通知和记录）
 */
@Entity(tableName = "medication_reminders")
@TypeConverters(Converters::class)
data class MedicationReminder @RequiresApi(Build.VERSION_CODES.O) constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val patientName: String,         // 患者姓名
    val medicineName: String,        // 药品名称
    val startDate: LocalDateTime,    // 开始服药日期
    val endDate: LocalDateTime,      // 结束服药日期
    val timesPerDay: Int,           // 每天服用次数
    val medicationTimes: List<LocalTime>, // 每天的服药时间点列表
    val dosageAmount: Float,        // 每次服用数量
    val dosageUnit: String,         // 剂量单位（片、袋、ml等）
    val instructions: String = "",   // 服药说明（如：饭前/饭后服用）
    val isActive: Boolean = true,   // 是否启用提醒
    var scheduledTime: LocalDateTime = LocalDateTime.now() // 当前计划服药时间
)

/**
 * Room 类型转换器
 */
class Converters {
    @RequiresApi(Build.VERSION_CODES.O)
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
