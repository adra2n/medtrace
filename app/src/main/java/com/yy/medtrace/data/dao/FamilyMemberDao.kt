package com.yy.medtrace.data.dao

import androidx.room.*
import com.yy.medtrace.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY isDefault DESC, id ASC")
    fun getAllMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members")
    suspend fun getAllMembersList(): List<FamilyMember>

    @Query("SELECT * FROM family_members WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultMember(): FamilyMember?

    @Query("SELECT * FROM family_members WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: Long): FamilyMember?

    @Insert
    suspend fun insert(member: FamilyMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<FamilyMember>)

    @Update
    suspend fun update(member: FamilyMember)

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM family_members")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM family_members")
    suspend fun count(): Int
}
