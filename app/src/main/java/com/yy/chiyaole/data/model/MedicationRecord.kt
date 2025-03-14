package com.yy.chiyaole.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yy.chiyaole.data.model.MedicationStatus
import java.time.LocalDateTime

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
    /**
     * 关联的提醒ID
     */
    val reminderId: Long,
    /**
     * 计划服药时间
     */
    val scheduledTime: LocalDateTime,
    /**
     * 实际服药时间
     */
    val actualTime: LocalDateTime?,
//    /**
//     * 延迟到的时间（如果状态为DELAYED）
//     */
//    val delayedTime: LocalDateTime?,
    /**
     * 服药状态
     */
    val status: MedicationStatus,
    /**
     * 备注（如：副作用、漏服原因等）
     */
    val note: String = "",
//    /**
//     * 延迟原因（如果状态为DELAYED）
//     */
//    val delayReason: String = ""
)
