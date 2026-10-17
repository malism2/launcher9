package com.malism.launcher9.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.malism.launcher9.di.AppContainer
import com.malism.launcher9.ui.auth.AuthViewModel
import com.malism.launcher9.ui.home.HomeViewModel
import com.malism.launcher9.ui.picker.AppPickerViewModel
import com.malism.launcher9.ui.settings.SettingsViewModel
import com.malism.launcher9.ui.setup.SetupViewModel
import com.malism.launcher9.ui.vault.VaultViewModel

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(container)
            AuthViewModel::class.java -> AuthViewModel(container.parentalRepository)
            AppPickerViewModel::class.java -> AppPickerViewModel(container)
            VaultViewModel::class.java -> VaultViewModel(container)
            SettingsViewModel::class.java -> SettingsViewModel(container)
            SetupViewModel::class.java -> SetupViewModel(container)
            else -> error("Unknown view model: ${modelClass.name}")
        }
        return viewModel as T
    }
}
