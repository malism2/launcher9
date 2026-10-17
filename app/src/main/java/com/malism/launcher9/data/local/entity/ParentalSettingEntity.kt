package com.malism.launcher9.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parental_setting")
data class ParentalSettingEntity(
    @PrimaryKey val id: Int = 1,
    val pinSalt: ByteArray,
    val pinHash: ByteArray,
    val biometricEnabled: Boolean,
    val setupComplete: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParentalSettingEntity) return false
        return id == other.id &&
            pinSalt.contentEquals(other.pinSalt) &&
            pinHash.contentEquals(other.pinHash) &&
            biometricEnabled == other.biometricEnabled &&
            setupComplete == other.setupComplete
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + pinSalt.contentHashCode()
        result = 31 * result + pinHash.contentHashCode()
        result = 31 * result + biometricEnabled.hashCode()
        result = 31 * result + setupComplete.hashCode()
        return result
    }
}
