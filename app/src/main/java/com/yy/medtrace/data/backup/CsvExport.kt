package com.yy.medtrace.data.backup

import android.content.Context
import android.content.Intent
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.model.MedicalRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val MED_COLUMNS = listOf(
    "成员", "诊断", "发病时间", "医院", "药品", "剂量", "频率", "疗程", "备注"
)

private fun escapeCsv(value: String): String {
    val v = value.replace("\"", "\"\"")
    return if (v.contains(",") || v.contains("\"") || v.contains("\n")) "\"$v\"" else v
}

suspend fun buildRecordsCsv(database: AppDatabase): String = withContext(Dispatchers.IO) {
    val members = database.familyMemberDao().getAllMembersList()
        .associateBy { it.id }
    val records = database.medicalRecordDao().getAllRecordsList()
        .sortedByDescending { it.onsetTime }

    val header = MED_COLUMNS.joinToString(",") { escapeCsv(it) }
    val rows = records.map { r -> recordToCsvRow(r, members[r.patientId]?.name ?: r.patientName) }
    (listOf(header) + rows + listOf("", "共 ${records.size} 条记录")).joinToString("\n")
}

private fun recordToCsvRow(r: MedicalRecord, memberName: String): String {
    val meds = r.medItems.joinToString("; ") { med ->
        listOf(med.name, med.dose, med.freq, med.duration).filter { it.isNotBlank() }
            .joinToString(" ")
    }
    return listOf(
        memberName,
        r.diagnosis,
        r.onsetTime.toString(),
        r.hospital,
        meds,
        r.dosage,
        r.frequency,
        "",
        r.notes
    ).joinToString(",") { escapeCsv(it) }
}

fun shareCsvIntent(context: Context, csv: String): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, "医迹医疗记录")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
}
