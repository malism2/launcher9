package com.malism.launcher9.di

import android.content.Context
import androidx.room.Room
import com.malism.launcher9.data.local.LauncherDatabase
import com.malism.launcher9.data.repository.InstalledAppRepository
import com.malism.launcher9.data.repository.ParentalRepository
import com.malism.launcher9.domain.model.LaunchableApp
import com.malism.launcher9.ui.icon.AppIconLoader
import com.malism.launcher9.util.DefaultHomeApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: LauncherDatabase = Room.databaseBuilder(
        context,
        LauncherDatabase::class.java,
        "launcher9.db",
    ).build()

    val installedAppRepository = InstalledAppRepository(context, appScope)
    val parentalRepository = ParentalRepository(context, database, appScope)
    val iconLoader = AppIconLoader(context)

    init {
        installedAppRepository.onPackageRemoved = { packageName ->
            parentalRepository.removeFromHome(packageName)
        }
        appScope.launch {
            parentalRepository.ensureReady()
            val apps = waitForInstalledApps()
            parentalRepository.seedDefaultHomeAppsIfNeeded(
                DefaultHomeApps.resolve(context, apps),
            )
        }
    }

    private suspend fun waitForInstalledApps(): List<LaunchableApp> {
        val current = installedAppRepository.apps.value
        if (current.isNotEmpty()) return current
        delay(300)
        if (installedAppRepository.apps.value.isNotEmpty()) {
            return installedAppRepository.apps.value
        }
        return withTimeoutOrNull(8_000) {
            installedAppRepository.apps.first { it.isNotEmpty() }
        } ?: installedAppRepository.apps.value
    }
}
