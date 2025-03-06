package com.yy.chiyaole.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yy.chiyaole.data.converter.DateTimeConverter
import com.yy.chiyaole.data.converter.LocalTimeConverter
import com.yy.chiyaole.data.dao.MedicalRecordDao
import com.yy.chiyaole.data.dao.MedicationReminderDao
import com.yy.chiyaole.data.dao.UserSettingsDao
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.model.MedicationReminder
import com.yy.chiyaole.data.model.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MedicationReminder::class,
        MedicalRecord::class,
        UserSettings::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class, LocalTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationReminderDao(): MedicationReminderDao
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 删除旧的user_settings表
                database.execSQL("DROP TABLE IF EXISTS user_settings")
                
                // 创建新的user_settings表，使用与Room实体相同的列名
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        sleepStartTime TEXT NOT NULL,
                        sleepEndTime TEXT NOT NULL,
                        enableVoiceReminder INTEGER NOT NULL,
                        enableNotificationSound INTEGER NOT NULL,
                        enableVibration INTEGER NOT NULL,
                        reminderAdvanceMinutes INTEGER NOT NULL,
                        darkMode INTEGER NOT NULL
                    )
                """)
                
                // 插入默认设置
                database.execSQL("""
                    INSERT OR REPLACE INTO user_settings (
                        id, sleepStartTime, sleepEndTime,
                        enableVoiceReminder, enableNotificationSound,
                        enableVibration, reminderAdvanceMinutes, darkMode
                    ) VALUES (
                        1, '22:00', '06:00',
                        1, 1, 1, 5, 0
                    )
                """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建临时表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS medication_reminders_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        patientName TEXT NOT NULL,
                        medicineName TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        endDate TEXT NOT NULL,
                        firstDoseTime TEXT NOT NULL,
                        intervalHours INTEGER NOT NULL,
                        timesPerDay INTEGER NOT NULL,
                        dosageAmount REAL NOT NULL,
                        dosageUnit TEXT NOT NULL,
                        instructions TEXT NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                """)
                
                // 迁移数据
                database.execSQL("""
                    INSERT INTO medication_reminders_temp (
                        id, patientName, medicineName, startDate, endDate,
                        firstDoseTime, intervalHours, timesPerDay, dosageAmount,
                        dosageUnit, instructions, isActive
                    )
                    SELECT 
                        id, patientName, medicineName, startDate, endDate,
                        firstDoseTime, intervalHours, 
                        CAST((24 / intervalHours) AS INTEGER), -- 根据间隔计算每天次数
                        1.0, -- 默认剂量为1
                        '片', -- 默认单位为片
                        instructions, isActive
                    FROM medication_reminders
                """)
                
                // 删除旧表
                database.execSQL("DROP TABLE medication_reminders")
                
                // 重命名新表
                database.execSQL("ALTER TABLE medication_reminders_temp RENAME TO medication_reminders")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                // 初始化默认设置
                                database.userSettingsDao().insertOrUpdate(UserSettings())
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
