package com.yy.medtrace.data.backup

import androidx.room.withTransaction
import com.yy.medtrace.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class BackupRepository(private val database: AppDatabase) {

    suspend fun exportAll(): BackupData = withContext(Dispatchers.IO) {
        val members = database.familyMemberDao().getAllMembersList()
        val records = database.medicalRecordDao().getAllRecordsList()
        val todos = database.healthTodoDao().getAllList()
        val settings = database.userSettingsDao().getUserSettings().firstOrNull()
        val schemaVersion = database.openHelper.readableDatabase.version
        BackupData(
            schemaVersion = schemaVersion,
            members = members,
            records = records,
            todos = todos,
            settings = settings
        )
    }

    suspend fun importAll(data: BackupData) = withContext(Dispatchers.IO) {
        val currentSchema = database.openHelper.readableDatabase.version
        if (data.schemaVersion != 0 && data.schemaVersion != currentSchema) {
            android.util.Log.w("BackupRepo", "Schema mismatch: backup v${data.schemaVersion}, current v$currentSchema")
        }
        database.withTransaction {
            database.familyMemberDao().clear()
            database.medicalRecordDao().clear()
            database.healthTodoDao().clear()
            database.familyMemberDao().insertAll(data.members)
            database.medicalRecordDao().insertAll(data.records)
            database.healthTodoDao().insertAll(data.todos)
            data.settings?.let { database.userSettingsDao().insertOrUpdate(it) }
        }
    }

    suspend fun exportExcel(): String = withContext(Dispatchers.IO) {
        val records = database.medicalRecordDao().getAllRecordsList()

        buildString {
            // BOM for Excel UTF-8 recognition
            append('\uFEFF')
            // Header row
            appendLine("\u5c31\u8bca\u7c7b\u578b,\u5c31\u8bca\u533b\u9662,\u5c31\u8bca\u65f6\u95f4,\u5bb6\u5ead\u6210\u5458,\u8bca\u65ad,\u5f00\u5177\u836f\u54c1,\u5907\u6ce8")
            for (record in records) {
                val medNames = record.medItems.joinToString("+") { it.name }
                val line = listOf(
                    record.diagnosis,
                    record.hospital,
                    record.onsetTime?.toString()?.replace("T", " ") ?: "",
                    record.patientName,
                    record.diagnosis,
                    medNames,
                    record.notes
                ).joinToString(",") { field ->
                    "\"${field.replace("\"", "\"\"")}\""
                }
                appendLine(line)
            }
        }
    }
}
