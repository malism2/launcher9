package com.malism.launcher9.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.launcher9.data.repository.ParentalRepository
import com.malism.launcher9.domain.model.PinResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthError {
    data object Wrong : AuthError
    data object NotSet : AuthError
    data class Locked(val seconds: Int) : AuthError
}

class AuthViewModel(
    private val parentalRepository: ParentalRepository,
) : ViewModel() {
    var pin by mutableStateOf("")
        private set
    var error by mutableStateOf<AuthError?>(null)
        private set
    var locked by mutableStateOf(false)
        private set

    val biometricEnabled = parentalRepository.settings
        .map { it?.biometricEnabled == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun append(digit: Int) {
        if (locked || pin.length >= 6) return
        pin += digit.toString()
        error = null
    }

    fun delete() {
        if (locked || pin.isEmpty()) return
        pin = pin.dropLast(1)
    }

    fun submit(onSuccess: () -> Unit) {
        if (locked || pin.length !in 4..6) return
        viewModelScope.launch {
            when (val result = parentalRepository.verifyPin(pin)) {
                PinResult.Ok -> onSuccess()
                PinResult.Wrong -> {
                    pin = ""
                    error = AuthError.Wrong
                }
                PinResult.NotSet -> error = AuthError.NotSet
                is PinResult.Locked -> {
                    pin = ""
                    locked = true
                    error = AuthError.Locked(result.seconds)
                }
            }
        }
    }
}
