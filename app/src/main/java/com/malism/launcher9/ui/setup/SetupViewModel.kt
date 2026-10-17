package com.malism.launcher9.ui.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.launcher9.di.AppContainer
import com.malism.launcher9.service.BlockServiceController
import com.malism.launcher9.util.DeviceState
import kotlinx.coroutines.launch

enum class SetupStep {
    Welcome,
    HomeRole,
    Pin,
    PinConfirm,
    Biometric,
    Usage,
    Overlay,
}

sealed interface SetupError {
    data object Short : SetupError
    data object Mismatch : SetupError
}

class SetupViewModel(
    private val container: AppContainer,
) : ViewModel() {
    var step by mutableStateOf(SetupStep.Welcome)
        private set
    var pin by mutableStateOf("")
        private set
    var confirm by mutableStateOf("")
        private set
    var error by mutableStateOf<SetupError?>(null)
        private set
    var biometricOn by mutableStateOf(false)
    var isDefaultHome by mutableStateOf(false)
        private set
    var usageGranted by mutableStateOf(false)
        private set
    var overlayGranted by mutableStateOf(false)
        private set

    val biometricAvailable = DeviceState.biometricAuthenticators(container.context) != null

    fun refresh() {
        val context = container.context
        isDefaultHome = DeviceState.isDefaultHome(context)
        usageGranted = DeviceState.hasUsageAccess(context)
        overlayGranted = DeviceState.canDrawOverlays(context)
    }

    fun append(digit: Int) {
        error = null
        if (step == SetupStep.PinConfirm) {
            if (confirm.length < 6) confirm += digit.toString()
        } else if (pin.length < 6) {
            pin += digit.toString()
        }
    }

    fun delete() {
        if (step == SetupStep.PinConfirm) {
            if (confirm.isNotEmpty()) confirm = confirm.dropLast(1)
        } else if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
        }
    }

    fun back() {
        error = null
        step = when (step) {
            SetupStep.Welcome -> SetupStep.Welcome
            SetupStep.HomeRole -> SetupStep.Welcome
            SetupStep.Pin -> SetupStep.HomeRole
            SetupStep.PinConfirm -> SetupStep.Pin
            SetupStep.Biometric -> SetupStep.PinConfirm
            SetupStep.Usage -> if (biometricAvailable) SetupStep.Biometric else SetupStep.PinConfirm
            SetupStep.Overlay -> SetupStep.Usage
        }
    }

    fun next(onFinished: () -> Unit) {
        when (step) {
            SetupStep.Welcome -> step = SetupStep.HomeRole
            SetupStep.HomeRole -> step = SetupStep.Pin
            SetupStep.Pin -> {
                if (pin.length !in 4..6) {
                    error = SetupError.Short
                } else {
                    error = null
                    step = SetupStep.PinConfirm
                }
            }
            SetupStep.PinConfirm -> {
                if (confirm != pin) {
                    confirm = ""
                    error = SetupError.Mismatch
                    return
                }
                viewModelScope.launch {
                    container.parentalRepository.setPin(pin, biometricOn)
                    step = if (biometricAvailable) SetupStep.Biometric else SetupStep.Usage
                }
            }
            SetupStep.Biometric -> viewModelScope.launch {
                container.parentalRepository.setBiometricEnabled(biometricOn)
                step = SetupStep.Usage
            }
            SetupStep.Usage -> step = SetupStep.Overlay
            SetupStep.Overlay -> viewModelScope.launch {
                container.parentalRepository.markSetupComplete()
                BlockServiceController.start(container.context)
                onFinished()
            }
        }
    }
}
