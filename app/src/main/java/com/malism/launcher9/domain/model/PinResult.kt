package com.malism.launcher9.domain.model

sealed interface PinResult {
    data object Ok : PinResult
    data object Wrong : PinResult
    data object NotSet : PinResult
    data class Locked(val seconds: Int) : PinResult
}
