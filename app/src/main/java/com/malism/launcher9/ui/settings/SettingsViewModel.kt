package com.malism.launcher9.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.launcher9.di.AppContainer
import com.malism.launcher9.domain.model.PinResult
import com.malism.launcher9.util.DeviceState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PinChangeStep {
    Idle,
    Current,
    New,
    Confirm,
}

sealed interface SettingsMessage {
    data object Wrong : SettingsMessage
    data object NotSet : SettingsMessage
    data object Short : SettingsMessage
    data object Mismatch : SettingsMessage
    data object Saved : SettingsMessage
    data class Locked(val seconds: Int) : SettingsMessage
}

class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {
    var pinStep by mutableStateOf(PinChangeStep.Idle)
        private set
    var draft by mutableStateOf("")
        private set
    var message by mutableStateOf<SettingsMessage?>(null)
        private set
    var usageGranted by mutableStateOf(false)
        private set
    var overlayGranted by mutableStateOf(false)
        private set
    var defaultHome by mutableStateOf(false)
        private set

    private var newPin = ""

    val biometricEnabled = container.parentalRepository.settings
        .map { it?.biometricEnabled == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val biometricAvailable = DeviceState.biometricAuthenticators(container.context) != null

    fun refresh() {
        val context = container.context
        usageGranted = DeviceState.hasUsageAccess(context)
        overlayGranted = DeviceState.canDrawOverlays(context)
        defaultHome = DeviceState.isDefaultHome(context)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.parentalRepository.setBiometricEnabled(enabled)
        }
    }

    fun startPinChange() {
        pinStep = PinChangeStep.Current
        draft = ""
        newPin = ""
        message = null
    }

    fun cancelPinChange() {
        pinStep = PinChangeStep.Idle
        draft = ""
        newPin = ""
        message = null
    }

    fun append(digit: Int) {
        if (draft.length >= 6) return
        draft += digit.toString()
        message = null
    }

    fun delete() {
        if (draft.isNotEmpty()) draft = draft.dropLast(1)
    }

    fun submitPinStep() {
        when (pinStep) {
            PinChangeStep.Idle -> Unit
            PinChangeStep.Current -> viewModelScope.launch {
                when (val result = container.parentalRepository.verifyPin(draft)) {
                    PinResult.Ok -> {
                        draft = ""
                        pinStep = PinChangeStep.New
                        message = null
                    }
                    PinResult.Wrong -> {
                        draft = ""
                        message = SettingsMessage.Wrong
                    }
                    PinResult.NotSet -> message = SettingsMessage.NotSet
                    is PinResult.Locked -> {
                        draft = ""
                        message = SettingsMessage.Locked(result.seconds)
                    }
                }
            }
            PinChangeStep.New -> {
                if (draft.length !in 4..6) {
                    message = SettingsMessage.Short
                } else {
                    newPin = draft
                    draft = ""
                    pinStep = PinChangeStep.Confirm
                    message = null
                }
            }
            PinChangeStep.Confirm -> {
                if (draft != newPin) {
                    draft = ""
                    message = SettingsMessage.Mismatch
                    return
                }
                viewModelScope.launch {
                    container.parentalRepository.replacePin(draft)
                    draft = ""
                    newPin = ""
                    pinStep = PinChangeStep.Idle
                    message = SettingsMessage.Saved
                }
            }
        }
    }
}
