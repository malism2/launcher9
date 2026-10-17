package com.malism.launcher9.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.malism.launcher9.data.local.dao.HomeAppDao
import com.malism.launcher9.data.local.dao.ParentalSettingDao
import com.malism.launcher9.data.local.entity.HomeAppEntity
import com.malism.launcher9.data.local.entity.ParentalSettingEntity

@Database(
    entities = [HomeAppEntity::class, ParentalSettingEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun homeAppDao(): HomeAppDao
    abstract fun parentalSettingDao(): ParentalSettingDao
}
