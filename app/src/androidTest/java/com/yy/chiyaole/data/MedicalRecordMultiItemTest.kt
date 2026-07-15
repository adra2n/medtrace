package com.yy.chiyaole.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yy.chiyaole.data.dao.MedicalRecordDao
import com.yy.chiyaole.data.model.MedicationItem
import com.yy.chiyaole.data.model.MedicalRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class MedicalRecordMultiItemTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: MedicalRecordDao

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.medicalRecordDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertMultipleMedItems_roundTrips() = runBlocking {
        val rec = MedicalRecord(
            id = 0,
            patientName = "测试",
            diagnosis = "感冒",
            onsetTime = LocalDateTime.now(),
            hospital = "某医院",
            medItems = listOf(
                MedicationItem("阿莫西林", "0.5g", "每日3次", "7天"),
                MedicationItem("布洛芬", "0.2g", "每日2次", "3天")
            ),
            frequency = "每日3次",
            dosage = "0.5g",
            notes = "多喝水"
        )
        dao.insert(rec)
        val all = dao.getRecentRecords(10).first()
        assertEquals(1, all.size)
        assertEquals(2, all[0].medItems.size)
        assertEquals("布洛芬", all[0].medItems[1].name)
    }
}
