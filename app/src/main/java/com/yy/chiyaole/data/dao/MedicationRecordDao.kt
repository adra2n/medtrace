package com.yy.chiyaole.data.dao

import androidx.room.*
import com.yy.chiyaole.data.model.MedicationRecord
import com.yy.chiyaole.data.model.MedicationStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MedicationRecordDao {
    @Query("SELECT * FROM medication_records ORDER BY scheduledTime DESC")
    fun getAll(): Flow<List<MedicationRecord>>

    @Query("""
        SELECT * FROM medication_records 
        WHERE scheduledTime >= :startTime AND scheduledTime < :endTime
        ORDER BY scheduledTime DESC
    """)
    fun getRecordsBetween(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<MedicationRecord>>

    @Query("""
        SELECT * FROM medication_records 
        WHERE date(scheduledTime) = date(:date)
        ORDER BY scheduledTime DESC
    """)
    fun getDayRecords(date: LocalDateTime): Flow<List<MedicationRecord>>

    @Query("""
        SELECT COUNT(*) FROM medication_records 
        WHERE status = :status AND scheduledTime >= :startTime AND scheduledTime < :endTime
    """)
    suspend fun getStatusCount(status: MedicationStatus, startTime: LocalDateTime, endTime: LocalDateTime): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MedicationRecord): Long

    @Update
    suspend fun update(record: MedicationRecord)

    @Delete
    suspend fun delete(record: MedicationRecord)

    @Query("DELETE FROM medication_records WHERE reminderId = :reminderId")
    suspend fun deleteByReminderId(reminderId: Long)

    @Transaction
    @Query("""
        SELECT * FROM medication_records 
        WHERE reminderId = :reminderId AND date(scheduledTime) = date(:date)
        ORDER BY scheduledTime DESC
    """)
    fun getReminderDayRecords(reminderId: Long, date: LocalDateTime): Flow<List<MedicationRecord>>

    @Query("""
        SELECT * FROM medication_records 
        WHERE reminderId = :reminderId AND date(scheduledTime) = date('now')
        ORDER BY scheduledTime DESC
        LIMIT 1
    """)
    fun getLatestRecordForReminder(reminderId: Long): Flow<MedicationRecord?>
}
