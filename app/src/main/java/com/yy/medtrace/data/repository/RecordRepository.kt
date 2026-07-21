package com.yy.medtrace.data.repository

import com.yy.medtrace.data.model.CountResult
import com.yy.medtrace.data.model.MedicalRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface RecordRepository {
    fun getAllRecords(): Flow<List<MedicalRecord>>
    fun getRecordsByMember(patientId: Long): Flow<List<MedicalRecord>>
    fun getUnknownRecords(): Flow<List<MedicalRecord>>
    suspend fun reassignToUnknown(memberId: Long)
    suspend fun getRecordById(id: Long): MedicalRecord?
    fun getRecentRecords(limit: Int): Flow<List<MedicalRecord>>
    fun searchByMember(
        patientId: Long,
        keyword: String?,
        likePattern: String,
        from: LocalDateTime,
        to: LocalDateTime
    ): Flow<List<MedicalRecord>>
    suspend fun getLatestRecord(): MedicalRecord?
    suspend fun insert(record: MedicalRecord): Long
    suspend fun count(): Int
    suspend fun update(record: MedicalRecord)
    suspend fun delete(record: MedicalRecord)
    suspend fun countByMembers(memberIds: List<Long>): List<CountResult>
}