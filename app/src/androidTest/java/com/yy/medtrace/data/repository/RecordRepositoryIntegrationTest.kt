package com.yy.medtrace.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yy.medtrace.data.AppDatabase
import com.yy.medtrace.data.dao.MedicalRecordDao
import com.yy.medtrace.data.model.MedicalRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class RecordRepositoryIntegrationTest {
    
    private lateinit var database: AppDatabase
    private lateinit var medicalRecordDao: MedicalRecordDao
    private lateinit var repository: RecordRepository
    
    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medicalRecordDao = database.medicalRecordDao()
        repository = RecordRepositoryImpl(medicalRecordDao)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `insert and retrieve record`() = runTest {
        val record = MedicalRecord(
            patientId = 1,
            diagnosis = "测试诊断",
            hospital = "测试医院",
            onsetTime = LocalDateTime.now()
        )
        val id = repository.insert(record)
        
        val retrieved = repository.getRecordById(id)
        assertNotNull(retrieved)
        assertEquals("测试诊断", retrieved?.diagnosis)
    }
    
    @Test
    fun `get all records should return flow`() = runTest {
        val record = MedicalRecord(
            patientId = 1,
            diagnosis = "测试诊断",
            hospital = "测试医院",
            onsetTime = LocalDateTime.now()
        )
        repository.insert(record)
        
        val records = repository.getAllRecords().first()
        assertEquals(1, records.size)
        assertEquals("测试诊断", records[0].diagnosis)
    }
    
    @Test
    fun `update record should work`() = runTest {
        val record = MedicalRecord(
            patientId = 1,
            diagnosis = "测试诊断",
            hospital = "测试医院",
            onsetTime = LocalDateTime.now()
        )
        val id = repository.insert(record)
        
        val retrieved = repository.getRecordById(id)
        assertNotNull(retrieved)
        
        val updatedRecord = retrieved!!.copy(diagnosis = "更新诊断")
        repository.update(updatedRecord)
        
        val updatedRetrieved = repository.getRecordById(id)
        assertNotNull(updatedRetrieved)
        assertEquals("更新诊断", updatedRetrieved?.diagnosis)
    }
    
    @Test
    fun `delete record should work`() = runTest {
        val record = MedicalRecord(
            patientId = 1,
            diagnosis = "测试诊断",
            hospital = "测试医院",
            onsetTime = LocalDateTime.now()
        )
        val id = repository.insert(record)
        
        val retrieved = repository.getRecordById(id)
        assertNotNull(retrieved)
        
        repository.delete(retrieved!!)
        
        val deletedRetrieved = repository.getRecordById(id)
        assertNull(deletedRetrieved)
    }
    
    @Test
    fun `count records should work`() = runTest {
        val initialCount = repository.count()
        
        val record = MedicalRecord(
            patientId = 1,
            diagnosis = "测试诊断",
            hospital = "测试医院",
            onsetTime = LocalDateTime.now()
        )
        repository.insert(record)
        
        val newCount = repository.count()
        assertEquals(initialCount + 1, newCount)
    }
}