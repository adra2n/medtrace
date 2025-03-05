package com.yy.chiyaole.data.dao

import androidx.room.*
import com.yy.chiyaole.data.model.MedicationReminder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MedicationReminderDao {
    @Query("SELECT * FROM medication_reminders ORDER BY firstDoseTime ASC")
    fun getAll(): Flow<List<MedicationReminder>>
    
    @Query("""
        SELECT * FROM medication_reminders 
        WHERE date(startDate) <= date(:today) 
        AND date(endDate) >= date(:today) 
        ORDER BY firstDoseTime ASC
    """)
    fun getTodayReminders(today: LocalDateTime): Flow<List<MedicationReminder>>
    
    @Query("SELECT * FROM medication_reminders WHERE id = :id")
    suspend fun getById(id: Long): MedicationReminder?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: MedicationReminder): Long
    
    @Update
    suspend fun update(reminder: MedicationReminder)
    
    @Delete
    suspend fun delete(reminder: MedicationReminder)
    
    @Query("DELETE FROM medication_reminders WHERE id = :id")
    suspend fun delete(id: Long)
}
