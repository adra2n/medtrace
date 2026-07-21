package com.yy.medtrace.data.repository

import com.yy.medtrace.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun getAllMembers(): Flow<List<FamilyMember>>
    suspend fun getAllMembersList(): List<FamilyMember>
    suspend fun getDefaultMember(): FamilyMember?
    suspend fun getMemberById(id: Long): FamilyMember?
    suspend fun insert(member: FamilyMember): Long
    suspend fun insertAll(members: List<FamilyMember>)
    suspend fun update(member: FamilyMember)
    suspend fun deleteById(id: Long)
    suspend fun clear()
}