package com.yy.chiyaole.data.dao

import androidx.room.*
import com.yy.chiyaole.data.model.MedicalRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM medical_records")
    fun getAllRecords(): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records")
    suspend fun getAllRecordsList(): List<MedicalRecord>

    @Query("SELECT * FROM medical_records WHERE patientId = :patientId ORDER BY onsetTime DESC")
    fun getRecordsByMember(patientId: Long): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE id = :id")
    suspend fun getRecordById(id: Long): MedicalRecord?

    @Query("SELECT * FROM medical_records ORDER BY onsetTime DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE patientId = :patientId ORDER BY onsetTime DESC LIMIT :limit")
    fun getRecentRecordsByMember(patientId: Long, limit: Int): Flow<List<MedicalRecord>>

    @Query(
        """
        SELECT * FROM medical_records
        WHERE patientId = :patientId
          AND (:keyword IS NULL OR :keyword = '' OR diagnosis LIKE :likePattern OR hospital LIKE :likePattern OR notes LIKE :likePattern)
          AND onsetTime >= :from AND onsetTime <= :to
        ORDER BY onsetTime DESC
        """
    )
    fun searchByMember(
        patientId: Long,
        keyword: String?,
        likePattern: String,
        from: java.time.LocalDateTime,
        to: java.time.LocalDateTime
    ): Flow<List<MedicalRecord>>

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

    @Insert
    suspend fun insertAll(records: List<MedicalRecord>)

    @Query("DELETE FROM medical_records")
    suspend fun clear()
}
