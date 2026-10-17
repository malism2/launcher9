package com.malism.launcher9.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.malism.launcher9.data.local.entity.HomeAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeAppDao {
    @Query("SELECT * FROM home_app ORDER BY sortOrder ASC")
    fun observe(): Flow<List<HomeAppEntity>>

    @Query("SELECT * FROM home_app ORDER BY sortOrder ASC")
    suspend fun getAll(): List<HomeAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apps: List<HomeAppEntity>)

    @Query("DELETE FROM home_app WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM home_app")
    suspend fun maxSort(): Int
}
