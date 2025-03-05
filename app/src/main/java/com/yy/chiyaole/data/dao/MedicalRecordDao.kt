package com.yy.chiyaole.data.dao

import androidx.room.*
import com.yy.chiyaole.data.model.MedicalRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM medical_records")
    fun getAllRecords(): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE id = :id")
    suspend fun getRecordById(id: Long): MedicalRecord?

    @Query("SELECT * FROM medical_records ORDER BY onsetTime DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<MedicalRecord>>

    @Insert
    suspend fun insert(record: MedicalRecord): Long

    @Update
    suspend fun update(record: MedicalRecord)

    @Delete
    suspend fun delete(record: MedicalRecord)
}
