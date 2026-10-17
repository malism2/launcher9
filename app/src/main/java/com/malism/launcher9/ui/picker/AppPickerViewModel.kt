package com.malism.launcher9.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.malism.launcher9.di.AppContainer
import com.malism.launcher9.domain.model.LaunchableApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppPickerViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages = selected.asStateFlow()

    val apps = combine(
        container.parentalRepository.homeApps,
        container.installedAppRepository.apps,
    ) { home, installed ->
        val onHome = home.map { it.packageName }.toSet()
        installed.filter { it.packageName !in onHome }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(packageName: String) {
        selected.update { current ->
            if (packageName in current) current - packageName else current + packageName
        }
    }

    fun addSelected(onDone: () -> Unit) {
        viewModelScope.launch {
            val chosen = apps.value.filter { it.packageName in selected.value }
            container.parentalRepository.addToHome(chosen)
            onDone()
        }
    }
}
