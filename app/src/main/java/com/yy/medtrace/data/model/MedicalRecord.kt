package com.yy.medtrace.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long = 0,
    val patientName: String,
    val diagnosis: String,
    val onsetTime: LocalDateTime,
    val hospital: String = "",
    val medItems: List<MedicationItem> = emptyList(),
    val notes: String = ""
)

data class CountResult(
    @ColumnInfo(name = "patientId")
    val patientId: Long,
    @ColumnInfo(name = "count")
    val count: Int
)
