package com.yy.chiyaole.data.dao

import androidx.room.*
import com.yy.chiyaole.data.model.MedicalRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM medical_records")
    fun getAllRecords(): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE patientId = :patientId ORDER BY onsetTime DESC")
    fun getRecordsByMember(patientId: Long): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE id = :id")
    suspend fun getRecordById(id: Long): MedicalRecord?

    @Query("SELECT * FROM medical_records ORDER BY onsetTime DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE patientId = :patientId ORDER BY onsetTime DESC LIMIT :limit")
    fun getRecentRecordsByMember(patientId: Long, limit: Int): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records ORDER BY onsetTime DESC LIMIT 1")
    suspend fun getLatestRecord(): MedicalRecord?

    @Insert
    suspend fun insert(record: MedicalRecord): Long

    @Query("SELECT COUNT(*) FROM medical_records")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM medical_records WHERE patientId = :patientId")
    suspend fun countByMember(patientId: Long): Int

    @Update
    suspend fun update(record: MedicalRecord)

    @Delete
    suspend fun delete(record: MedicalRecord)
}
