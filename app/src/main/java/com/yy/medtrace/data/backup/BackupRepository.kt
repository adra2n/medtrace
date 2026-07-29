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
}
