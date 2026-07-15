package com.yy.chiyaole.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yy.chiyaole.data.converter.LocalDateConverter
import com.yy.chiyaole.data.converter.LocalDateTimeConverter
import com.yy.chiyaole.data.converter.LocalTimeConverter
import com.yy.chiyaole.data.converter.LocalTimeListConverter
import com.yy.chiyaole.data.converter.MedicationItemListConverter
import com.yy.chiyaole.data.dao.MedicalRecordDao
import com.yy.chiyaole.data.dao.UserSettingsDao
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.model.UserSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MedicalRecord::class,
        UserSettings::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(
    LocalDateConverter::class,
    LocalDateTimeConverter::class,
    LocalTimeConverter::class,
    LocalTimeListConverter::class,
    MedicationItemListConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
//        private val MIGRATION_1_2 = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                // 创建新的用户设置表，不包含睡眠时间字段
//                database.execSQL("""
//                    CREATE TABLE IF NOT EXISTS user_settings_new (
//                        id INTEGER PRIMARY KEY NOT NULL,
//                        enableVoiceReminder INTEGER NOT NULL,
//                        enableNotificationSound INTEGER NOT NULL,
//                        enableVibration INTEGER NOT NULL,
//                        reminderAdvanceMinutes INTEGER NOT NULL,
//                        darkMode INTEGER NOT NULL
//                    )
//                """)
//
//                // 复制旧数据到新表，忽略睡眠时间字段
//                database.execSQL("""
//                    INSERT INTO user_settings_new (
//                        id, enableVoiceReminder, enableNotificationSound,
//                        enableVibration, reminderAdvanceMinutes, darkMode
//                    )
//                    SELECT id, enableVoiceReminder, enableNotificationSound,
//                           enableVibration, reminderAdvanceMinutes, darkMode
//                    FROM user_settings
//                """)
//
//                // 删除旧表
//                database.execSQL("DROP TABLE user_settings")
//
//                // 重命名新表
//                database.execSQL("ALTER TABLE user_settings_new RENAME TO user_settings")
//            }
//        }
//
//        private val MIGRATION_2_3 = object : Migration(2, 3) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                // 创建服药记录表
//                database.execSQL("""
//                    CREATE TABLE IF NOT EXISTS medication_records (
//                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
//                        reminderId INTEGER NOT NULL,
//                        scheduledTime TEXT NOT NULL,
//                        actualTime TEXT,
//                        status TEXT NOT NULL,
//                        note TEXT NOT NULL DEFAULT '',
//                        FOREIGN KEY (reminderId) REFERENCES medication_reminders(id) ON DELETE CASCADE
//                    )
//                """)
//
//                // 创建 reminderId 列的索引
//                database.execSQL("""
//                    CREATE INDEX IF NOT EXISTS index_medication_records_reminderId
//                    ON medication_records(reminderId)
//                """)
//            }
//        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
//                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
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
