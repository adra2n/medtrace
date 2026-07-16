package com.yy.chiyaole.data.backup

import androidx.room.withTransaction
import com.yy.chiyaole.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class BackupRepository(private val database: AppDatabase) {

    suspend fun exportAll(): BackupData = withContext(Dispatchers.IO) {
        val members = database.familyMemberDao().getAllMembersList()
        val records = database.medicalRecordDao().getAllRecordsList()
        val settings = database.userSettingsDao().getUserSettings().firstOrNull()
        BackupData(
            members = members,
            records = records,
            settings = settings
        )
    }

    suspend fun importAll(data: BackupData) = withContext(Dispatchers.IO) {
        database.withTransaction {
            // 成员保留原始 id，使医疗记录的 patientId 外键引用在恢复后仍有效
            database.familyMemberDao().clear()
            database.medicalRecordDao().clear()
            database.familyMemberDao().insertAll(data.members)
            database.medicalRecordDao().insertAll(data.records)
            data.settings?.let { database.userSettingsDao().insertOrUpdate(it) }
        }
    }
}
