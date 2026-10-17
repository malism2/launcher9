package com.malism.launcher9

import android.app.Application
import com.malism.launcher9.di.AppContainer

class LauncherApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.installedAppRepository.start()
    }
}
