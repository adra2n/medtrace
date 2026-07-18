package com.yy.medtrace.data.dao

import androidx.room.*
import com.yy.medtrace.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getUserSettings(): Flow<UserSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettings)

    @Query("SELECT EXISTS(SELECT 1 FROM user_settings WHERE id = 1)")
    suspend fun hasSettings(): Boolean
}
