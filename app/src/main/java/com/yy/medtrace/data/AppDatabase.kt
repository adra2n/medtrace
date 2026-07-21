package com.yy.medtrace.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yy.medtrace.data.converter.LocalDateConverter
import com.yy.medtrace.data.converter.LocalDateTimeConverter
import com.yy.medtrace.data.converter.LocalTimeConverter
import com.yy.medtrace.data.converter.LocalTimeListConverter
import com.yy.medtrace.data.converter.MedicationItemListConverter
import com.yy.medtrace.data.dao.FamilyMemberDao
import com.yy.medtrace.data.dao.HealthTodoDao
import com.yy.medtrace.data.dao.MedicalRecordDao
import com.yy.medtrace.data.dao.UserSettingsDao
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.model.UserSettings
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
        FamilyMember::class,
        HealthTodo::class
    ],
    version = 11,
    exportSchema = true
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
    abstract fun healthTodoDao(): HealthTodoDao

    companion object {
        internal val MIGRATION_6_7 = object : Migration(6, 7) {
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

        internal val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // medical_records: 新增 AI 解析指标字段（加列不丢数据）
                database.execSQL("ALTER TABLE medical_records ADD COLUMN metrics_json TEXT NOT NULL DEFAULT ''")
            }
        }

        internal val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 健康待办表
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS health_todos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        memberId INTEGER NOT NULL DEFAULT 0,
                        memberName TEXT NOT NULL DEFAULT '',
                        content TEXT NOT NULL,
                        dueDate TEXT NOT NULL DEFAULT '',
                        done INTEGER NOT NULL DEFAULT 0
                    )"""
                )
            }
        }

        internal val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // family_members: 新增头像本地路径字段（加列不丢数据）
                database.execSQL("ALTER TABLE family_members ADD COLUMN avatarPath TEXT NOT NULL DEFAULT ''")
            }
        }

        internal val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // health_todos: 新增已通知日期字段（用于每日提醒防重，随备份恢复）
                database.execSQL("ALTER TABLE health_todos ADD COLUMN notifiedDate TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 标记：因缺少历史迁移（v1–v5 schema 已丢失）导致旧库被重置重建，
        // 用户需从加密备份（GitHub Gist / 文件）恢复数据。供 UI 一次性提示。
        @Volatile
        var migrationResetHappened: Boolean = false
            private set

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    buildDatabase(context)
                } catch (e: IllegalStateException) {
                    // 迁移缺失（如老预发布用户 v1–v5 升级）：无法迁移即重置，避免崩溃。
                    // 删除旧库后重建为当前 schema，数据丢失需用户从备份恢复。
                    context.applicationContext.deleteDatabase("app_database")
                    migrationResetHappened = true
                    buildDatabase(context)
                }
                INSTANCE = instance
                instance
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "app_database"
            )
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                    }
                })
                .build()
        }
    }
}
