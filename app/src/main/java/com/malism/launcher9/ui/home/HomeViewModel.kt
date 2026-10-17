package com.malism.launcher9.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.launcher9.di.AppContainer
import com.malism.launcher9.domain.model.LaunchableApp
import com.malism.launcher9.util.DeviceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomePlacement(
    val slot: Int,
    val app: LaunchableApp,
)

sealed interface HomePinError {
    data object Short : HomePinError
    data object Mismatch : HomePinError
}

class HomeViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val placements = combine(
        container.parentalRepository.homeApps,
        container.installedAppRepository.apps,
    ) { home, installed ->
        val byPackage = installed.associateBy { it.packageName }
        home.mapNotNull { entity ->
            val app = byPackage[entity.packageName] ?: return@mapNotNull null
            HomePlacement(slot = entity.sortOrder, app = app)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pinConfigured = container.parentalRepository.pinConfigured

    val showPermissionWarning = flow {
        while (true) {
            val context = container.context
            emit(
                !DeviceState.hasUsageAccess(context) || !DeviceState.canDrawOverlays(context),
            )
            delay(1_500)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isDefaultHome = flow {
        while (true) {
            emit(DeviceState.isDefaultHome(container.context))
            delay(1_500)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    var pinDraft by mutableStateOf("")
        private set
    var pinConfirm by mutableStateOf("")
        private set
    var pinConfirming by mutableStateOf(false)
        private set
    var pinError by mutableStateOf<HomePinError?>(null)
        private set

    fun launch(app: LaunchableApp) {
        container.installedAppRepository.launch(app)
    }

    fun moveApp(packageName: String, toSlot: Int) {
        viewModelScope.launch {
            container.parentalRepository.moveHomeApp(packageName, toSlot)
        }
    }

    fun resetPinSetup() {
        pinDraft = ""
        pinConfirm = ""
        pinConfirming = false
        pinError = null
    }

    fun appendPinDigit(digit: Int) {
        pinError = null
        if (pinConfirming) {
            if (pinConfirm.length < 6) pinConfirm += digit.toString()
        } else if (pinDraft.length < 6) {
            pinDraft += digit.toString()
        }
    }

    fun deletePinDigit() {
        if (pinConfirming) {
            if (pinConfirm.isNotEmpty()) pinConfirm = pinConfirm.dropLast(1)
        } else if (pinDraft.isNotEmpty()) {
            pinDraft = pinDraft.dropLast(1)
        }
    }

    fun submitPinSetup(onSuccess: () -> Unit) {
        if (!pinConfirming) {
            if (pinDraft.length !in 4..6) {
                pinError = HomePinError.Short
                return
            }
            pinConfirming = true
            pinError = null
            return
        }
        if (pinConfirm != pinDraft) {
            pinConfirm = ""
            pinError = HomePinError.Mismatch
            return
        }
        viewModelScope.launch {
            container.parentalRepository.setPin(pinDraft)
            resetPinSetup()
            onSuccess()
        }
    }
}
