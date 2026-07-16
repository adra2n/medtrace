package com.yy.chiyaole.data.backup

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
        database.familyMemberDao().clear()
        database.medicalRecordDao().clear()
        database.familyMemberDao().insertAll(data.members)
        database.medicalRecordDao().insertAll(data.records)
        data.settings?.let { database.userSettingsDao().insertOrUpdate(it) }
    }
}
