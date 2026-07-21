package com.yy.medtrace.data.repository

import com.yy.medtrace.data.dao.FamilyMemberDao
import com.yy.medtrace.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

class MemberRepositoryImpl(private val familyMemberDao: FamilyMemberDao) : MemberRepository {
    
    override fun getAllMembers(): Flow<List<FamilyMember>> {
        return familyMemberDao.getAllMembers()
    }
    
    override suspend fun getAllMembersList(): List<FamilyMember> {
        return familyMemberDao.getAllMembersList()
    }
    
    override suspend fun getDefaultMember(): FamilyMember? {
        return familyMemberDao.getDefaultMember()
    }
    
    override suspend fun getMemberById(id: Long): FamilyMember? {
        return familyMemberDao.getMemberById(id)
    }
    
    override suspend fun insert(member: FamilyMember): Long {
        return familyMemberDao.insert(member)
    }
    
    override suspend fun insertAll(members: List<FamilyMember>) {
        familyMemberDao.insertAll(members)
    }
    
    override suspend fun update(member: FamilyMember) {
        familyMemberDao.update(member)
    }
    
    override suspend fun deleteById(id: Long) {
        familyMemberDao.deleteById(id)
    }
    
    override suspend fun clear() {
        familyMemberDao.clear()
    }
}