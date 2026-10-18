package com.malism.launcher9.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Resources

data class ForegroundTarget(
    val packageName: String,
    val className: String?,
)

object ForegroundAppReader {
    private val recents: ComponentName? by lazy { loadRecentsComponent() }

    fun isRecents(target: ForegroundTarget?): Boolean {
        val component = recents ?: return false
        if (target == null) return false
        return target.packageName == component.packageName && target.className == component.className
    }

    fun read(context: Context): ForegroundTarget? {
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val eventTarget = newestResume(usageStats, end - 8_000L, end)
        if (eventTarget != null) return eventTarget
        val stats = usageStats.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 60_000L, end)
        val packageName = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return null
        return ForegroundTarget(packageName, null)
    }

    private fun loadRecentsComponent(): ComponentName? {
        val resources = Resources.getSystem()
        val id = resources.getIdentifier("config_recentsComponentName", "string", "android")
        if (id == 0) return null
        val flat = resources.getString(id)
        if (flat.isBlank()) return null
        return ComponentName.unflattenFromString(flat)
    }

    private fun newestResume(usageStats: UsageStatsManager, start: Long, end: Long): ForegroundTarget? {
        val events = usageStats.queryEvents(start, end) ?: return null
        val event = UsageEvents.Event()
        var target: ForegroundTarget? = null
        var time = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // 1 is ACTIVITY_RESUMED. The public constant arrived in API 29; the value is older.
            val packageName = event.packageName
            if (event.eventType == 1 && packageName != null && event.timeStamp >= time) {
                target = ForegroundTarget(packageName, event.className)
                time = event.timeStamp
            }
        }
        return target
    }
}
