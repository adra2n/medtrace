package com.yy.chiyaole.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class MedicationStatus {
    TAKEN,      // 已服用
    SKIPPED,    // 已跳过
    PENDING     // 未操作
}

@Entity(
    tableName = "medication_records",
    foreignKeys = [
        ForeignKey(
            entity = MedicationReminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reminderId"])
    ]
)
data class MedicationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: Long,          // 关联的提醒ID
    val scheduledTime: LocalDateTime, // 计划服药时间
    val actualTime: LocalDateTime?,   // 实际服药时间
    val status: MedicationStatus,     // 服药状态：已服用、已跳过、未操作
    val note: String = ""            // 备注（如：副作用、漏服原因等）
)
