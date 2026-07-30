package com.yy.medtrace.data.repository

import com.yy.medtrace.data.dao.MedicalRecordDao
import com.yy.medtrace.data.model.CountResult
import com.yy.medtrace.data.model.MedicalRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class RecordRepositoryImpl(private val medicalRecordDao: MedicalRecordDao) : RecordRepository {
    
    override fun getAllRecords(): Flow<List<MedicalRecord>> {
        return medicalRecordDao.getAllRecords()
    }
    
    override suspend fun getAllRecordsList(): List<MedicalRecord> {
        return medicalRecordDao.getAllRecordsList()
    }
    
    override fun getRecordsByMember(patientId: Long): Flow<List<MedicalRecord>> {
        return medicalRecordDao.getRecordsByMember(patientId)
    }
    
    override fun getUnknownRecords(): Flow<List<MedicalRecord>> {
        return medicalRecordDao.getUnknownRecords()
    }
    
    override suspend fun reassignToUnknown(memberId: Long) {
        medicalRecordDao.reassignToUnknown(memberId)
    }
    
    override suspend fun getRecordById(id: Long): MedicalRecord? {
        return medicalRecordDao.getRecordById(id)
    }
    
    override fun getRecentRecords(limit: Int): Flow<List<MedicalRecord>> {
        return medicalRecordDao.getRecentRecords(limit)
    }
    
    override fun getRecentRecordsByMember(patientId: Long, limit: Int): Flow<List<MedicalRecord>> {
        return medicalRecordDao.getRecentRecordsByMember(patientId, limit)
    }
    
    override fun searchByMember(
        patientId: Long,
        keyword: String?,
        likePattern: String,
        from: LocalDateTime,
        to: LocalDateTime
    ): Flow<List<MedicalRecord>> {
        return medicalRecordDao.searchByMember(patientId, keyword, likePattern, from, to)
    }
    
    override suspend fun searchByMemberPaged(
        patientId: Long,
        keyword: String?,
        likePattern: String,
        from: LocalDateTime,
        to: LocalDateTime,
        limit: Int,
        offset: Int
    ): List<MedicalRecord> {
        return medicalRecordDao.searchByMemberPaged(patientId, keyword, likePattern, from, to, limit, offset)
    }
    
    override suspend fun getLatestRecord(): MedicalRecord? {
        return medicalRecordDao.getLatestRecord()
    }
    
    override suspend fun insert(record: MedicalRecord): Long {
        return medicalRecordDao.insert(record)
    }
    
    override suspend fun count(): Int {
        return medicalRecordDao.count()
    }
    
    override suspend fun countByMember(patientId: Long): Int {
        return medicalRecordDao.countByMember(patientId)
    }
    
    override suspend fun update(record: MedicalRecord) {
        medicalRecordDao.update(record)
    }
    
    override suspend fun delete(record: MedicalRecord) {
        medicalRecordDao.delete(record)
    }
    
    override suspend fun insertAll(records: List<MedicalRecord>) {
        medicalRecordDao.insertAll(records)
    }
    
    override suspend fun clear() {
        medicalRecordDao.clear()
    }
    
    override suspend fun countByMembers(memberIds: List<Long>): List<CountResult> {
        return medicalRecordDao.countByMembers(memberIds)
    }
}