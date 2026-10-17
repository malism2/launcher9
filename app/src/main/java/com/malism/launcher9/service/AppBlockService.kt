package com.malism.launcher9.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.malism.launcher9.LauncherApp
import com.malism.launcher9.R
import com.malism.launcher9.util.DeviceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppBlockService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var previousPackage: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (monitorJob == null) {
            monitorJob = scope.launch { monitor() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        OverlayShield.hide(this)
        super.onDestroy()
    }

    private suspend fun monitor() {
        val parental = (application as LauncherApp).container.parentalRepository
        if (!parental.isSetupComplete()) {
            stopSelf()
            return
        }
        while (scope.isActive) {
            try {
                if (!DeviceState.hasUsageAccess(this)) {
                    delay(1500)
                    continue
                }
                val current = ForegroundAppReader.read(this)
                parental.onForegroundChanged(previousPackage, current)
                if (current != null) previousPackage = current
                if (parental.shouldBlock(current) && current != null) {
                    withContext(Dispatchers.Main) {
                        if (!BlockUi.visible) {
                            BlockUi.visible = true
                            OverlayShield.show(applicationContext)
                            BlockServiceController.openBlock(this@AppBlockService, current)
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep the monitor alive if one sample fails.
            }
            delay(700)
        }
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_home)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "block_monitor"
        const val NOTIFICATION_ID = 9
    }
}
