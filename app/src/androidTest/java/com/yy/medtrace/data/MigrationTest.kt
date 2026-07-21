package com.yy.medtrace.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    private val TEST_DB_NAME = "migration-test"
    
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )
    
    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        // 创建版本6的数据库
        var db = helper.createDatabase(TEST_DB_NAME, 6)
        
        // 验证版本6的表结构
        db.query("SELECT * FROM family_members").use { cursor ->
            // 验证列存在
            assert(cursor.getColumnIndex("gender") == -1) // 版本6没有gender列
        }
        
        // 执行迁移
        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            7,
            true,
            AppDatabase.MIGRATION_6_7
        )
        
        // 验证版本7的表结构
        db.query("SELECT * FROM family_members").use { cursor ->
            // 验证新列存在
            assert(cursor.getColumnIndex("gender") != -1)
            assert(cursor.getColumnIndex("birthday") != -1)
            assert(cursor.getColumnIndex("bloodType") != -1)
            assert(cursor.getColumnIndex("allergy") != -1)
            assert(cursor.getColumnIndex("chronic") != -1)
            assert(cursor.getColumnIndex("medicationNote") != -1)
            assert(cursor.getColumnIndex("otherNote") != -1)
        }
        
        db.query("SELECT * FROM user_settings").use { cursor ->
            assert(cursor.getColumnIndex("selectedMemberId") != -1)
        }
        
        db.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        // 创建版本7的数据库
        var db = helper.createDatabase(TEST_DB_NAME, 7)
        
        // 执行迁移
        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            8,
            true,
            AppDatabase.MIGRATION_7_8
        )
        
        // 验证版本8的表结构
        db.query("SELECT * FROM medical_records").use { cursor ->
            assert(cursor.getColumnIndex("metrics_json") != -1)
        }
        
        db.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun migrate8To9() {
        // 创建版本8的数据库
        var db = helper.createDatabase(TEST_DB_NAME, 8)
        
        // 执行迁移
        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            9,
            true,
            AppDatabase.MIGRATION_8_9
        )
        
        // 验证版本9的表结构
        db.query("SELECT * FROM health_todos").use { cursor ->
            assert(cursor.getColumnIndex("id") != -1)
            assert(cursor.getColumnIndex("memberId") != -1)
            assert(cursor.getColumnIndex("memberName") != -1)
            assert(cursor.getColumnIndex("content") != -1)
            assert(cursor.getColumnIndex("dueDate") != -1)
            assert(cursor.getColumnIndex("done") != -1)
        }
        
        db.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun migrate9To10() {
        // 创建版本9的数据库
        var db = helper.createDatabase(TEST_DB_NAME, 9)
        
        // 执行迁移
        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            10,
            true,
            AppDatabase.MIGRATION_9_10
        )
        
        // 验证版本10的表结构
        db.query("SELECT * FROM family_members").use { cursor ->
            assert(cursor.getColumnIndex("avatarPath") != -1)
        }
        
        db.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun migrate10To11() {
        // 创建版本10的数据库
        var db = helper.createDatabase(TEST_DB_NAME, 10)
        
        // 执行迁移
        db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            11,
            true,
            AppDatabase.MIGRATION_10_11
        )
        
        // 验证版本11的表结构
        db.query("SELECT * FROM health_todos").use { cursor ->
            assert(cursor.getColumnIndex("notifiedDate") != -1)
        }
        
        db.close()
    }
}