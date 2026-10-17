package com.malism.launcher9.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

object ForegroundAppReader {
    fun read(context: Context): String? {
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val eventPackage = newestResume(usageStats, end - 8_000L, end)
        if (eventPackage != null) return eventPackage
        val stats = usageStats.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 60_000L, end)
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun newestResume(usageStats: UsageStatsManager, start: Long, end: Long): String? {
        val events = usageStats.queryEvents(start, end) ?: return null
        val event = UsageEvents.Event()
        var packageName: String? = null
        var time = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // 1 is ACTIVITY_RESUMED. The public constant arrived in API 29; the value is older.
            if (event.eventType == 1 && event.timeStamp >= time) {
                packageName = event.packageName
                time = event.timeStamp
            }
        }
        return packageName
    }
}
