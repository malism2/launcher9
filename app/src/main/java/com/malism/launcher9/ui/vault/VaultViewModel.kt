package com.malism.launcher9.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.launcher9.di.AppContainer
import com.malism.launcher9.domain.model.LaunchableApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class VaultViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val apps = container.installedAppRepository.apps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun open(app: LaunchableApp) {
        container.parentalRepository.allowSession(app.packageName)
        container.installedAppRepository.launch(app)
    }
}
