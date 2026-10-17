package com.malism.launcher9.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.malism.launcher9.LauncherApp
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as LauncherApp
        app.container.appScope.launch {
            try {
                if (app.container.parentalRepository.isSetupComplete()) {
                    BlockServiceController.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
