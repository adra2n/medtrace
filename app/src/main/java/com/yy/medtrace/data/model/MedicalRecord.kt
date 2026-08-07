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
    val frequency: String,    // 服药频率，如"每天三次"
    val dosage: String,       // 用药剂量，如"每次一片"
    val notes: String = "",
    @ColumnInfo(name = "metrics_json")
    val metricsJson: String = "",  // AI 解析的检查指标 JSON（Metric 列表），用于健康趋势
    @ColumnInfo(name = "attachmentPath")
    val attachmentPath: String = "",  // 附件图片本地路径
    @ColumnInfo(name = "visit_type", defaultValue = "")
    val visitType: String = ""  // 就诊类型：门诊、急诊、体检、复查、自购药、其他
)

data class CountResult(
    @ColumnInfo(name = "patientId")
    val patientId: Long,
    @ColumnInfo(name = "count")
    val count: Int
)
