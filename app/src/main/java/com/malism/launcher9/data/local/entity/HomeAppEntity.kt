package com.malism.launcher9.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_app")
data class HomeAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val sortOrder: Int,
    val addedAt: Long,
)
