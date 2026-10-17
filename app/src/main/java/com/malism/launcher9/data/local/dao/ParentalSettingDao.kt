package com.malism.launcher9.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.malism.launcher9.data.local.entity.ParentalSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentalSettingDao {
    @Query("SELECT * FROM parental_setting WHERE id = 1")
    fun observe(): Flow<ParentalSettingEntity?>

    @Query("SELECT * FROM parental_setting WHERE id = 1")
    suspend fun get(): ParentalSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ParentalSettingEntity)
}
