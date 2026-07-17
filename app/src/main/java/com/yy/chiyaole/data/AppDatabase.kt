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
import com.yy.chiyaole.data.dao.FamilyMemberDao
import com.yy.chiyaole.data.dao.MedicalRecordDao
import com.yy.chiyaole.data.dao.UserSettingsDao
import com.yy.chiyaole.data.model.FamilyMember
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
        UserSettings::class,
        FamilyMember::class
    ],
    version = 8,
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
    abstract fun familyMemberDao(): FamilyMemberDao

    companion object {
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // family_members: 新增结构化个人/医疗信息字段（加列不丢数据）
                database.execSQL("ALTER TABLE family_members ADD COLUMN gender TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE family_members ADD COLUMN birthday TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE family_members ADD COLUMN bloodType TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE family_members ADD COLUMN allergy TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE family_members ADD COLUMN chronic TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE family_members ADD COLUMN medicationNote TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE family_members ADD COLUMN otherNote TEXT NOT NULL DEFAULT ''")
                // user_settings: 新增当前选中成员字段
                database.execSQL("ALTER TABLE user_settings ADD COLUMN selectedMemberId INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // medical_records: 新增 AI 解析指标字段（加列不丢数据）
                database.execSQL("ALTER TABLE medical_records ADD COLUMN metrics_json TEXT NOT NULL DEFAULT ''")
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
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
