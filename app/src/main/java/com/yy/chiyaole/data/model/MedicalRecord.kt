package com.yy.chiyaole.data.model

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
    val frequency: String,    // 服药频率，如"每天三次"
    val dosage: String,       // 用药剂量，如"每次一片"
    val notes: String = ""
)
