package com.yy.medtrace.data.repository

import com.yy.medtrace.data.model.CountResult
import com.yy.medtrace.data.model.MedicalRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface RecordRepository {
    fun getAllRecords(): Flow<List<MedicalRecord>>
    suspend fun getAllRecordsList(): List<MedicalRecord>
    fun getRecordsByMember(patientId: Long): Flow<List<MedicalRecord>>
    fun getUnknownRecords(): Flow<List<MedicalRecord>>
    suspend fun reassignToUnknown(memberId: Long)
    suspend fun getRecordById(id: Long): MedicalRecord?
    fun getRecentRecords(limit: Int): Flow<List<MedicalRecord>>
    fun getRecentRecordsByMember(patientId: Long, limit: Int): Flow<List<MedicalRecord>>

    /** 每位成员最近一次就诊记录（每人最多一条），用于首页「最新记录」。 */
    fun getLatestRecordPerMember(): Flow<List<MedicalRecord>>
    fun searchByMember(
        patientId: Long,
        keyword: String?,
        likePattern: String,
        from: LocalDateTime,
        to: LocalDateTime
    ): Flow<List<MedicalRecord>>
    suspend fun searchByMemberPaged(
        patientId: Long,
        keyword: String?,
        likePattern: String,
        from: LocalDateTime,
        to: LocalDateTime,
        limit: Int,
        offset: Int
    ): List<MedicalRecord>

    /** 全部成员的记录分页查询（不按 patientId 过滤）。 */
    suspend fun searchAllPaged(
        keyword: String?,
        likePattern: String,
        from: LocalDateTime,
        to: LocalDateTime,
        limit: Int,
        offset: Int
    ): List<MedicalRecord>

    suspend fun getLatestRecord(): MedicalRecord?
    suspend fun insert(record: MedicalRecord): Long
    suspend fun count(): Int
    suspend fun countByMember(patientId: Long): Int
    suspend fun update(record: MedicalRecord)
    suspend fun delete(record: MedicalRecord)
    suspend fun insertAll(records: List<MedicalRecord>)
    suspend fun clear()
    suspend fun countByMembers(memberIds: List<Long>): List<CountResult>
}