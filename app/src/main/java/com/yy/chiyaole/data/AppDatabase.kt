package com.yy.chiyaole.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yy.chiyaole.data.converter.DateTimeConverter
import com.yy.chiyaole.data.dao.MedicalRecordDao
import com.yy.chiyaole.data.dao.MedicationReminderDao
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.model.MedicationReminder

@Database(
    entities = [MedicationReminder::class, MedicalRecord::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun medicalRecordDao(): MedicalRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
