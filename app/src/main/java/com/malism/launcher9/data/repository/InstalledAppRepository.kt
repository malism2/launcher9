package com.malism.launcher9.data.repository

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import com.malism.launcher9.domain.model.LaunchableApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

class InstalledAppRepository(
    context: Context,
    private val appScope: CoroutineScope,
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val selfPackage = context.packageName
    private val collator = Collator.getInstance(Locale.CHINA)
    private val appsState = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val apps: StateFlow<List<LaunchableApp>> = appsState.asStateFlow()

    var onPackageRemoved: (suspend (String) -> Unit)? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            refresh()
            appScope.launch { onPackageRemoved?.invoke(packageName) }
        }

        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()
    }

    fun start() {
        refresh()
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
    }

    fun refresh() {
        appScope.launch(Dispatchers.IO) {
            appsState.value = load()
        }
    }

    fun launch(app: LaunchableApp) {
        launcherApps.startMainActivity(app.component, Process.myUserHandle(), null, null)
    }

    private fun load(): List<LaunchableApp> {
        val activities = try {
            launcherApps.getActivityList(null, Process.myUserHandle())
        } catch (_: SecurityException) {
            emptyList()
        }
        return activities
            .filter { it.componentName.packageName != selfPackage }
            .groupBy { it.componentName.packageName }
            .map { (_, infos) ->
                val info = infos.first()
                LaunchableApp(
                    packageName = info.componentName.packageName,
                    component = info.componentName,
                    label = info.label?.toString().orEmpty().ifBlank { info.componentName.packageName },
                )
            }
            .sortedWith { left, right -> collator.compare(left.label, right.label) }
    }
}
