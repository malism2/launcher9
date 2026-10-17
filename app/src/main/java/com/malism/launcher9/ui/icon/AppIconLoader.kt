package com.malism.launcher9.ui.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.LruCache

class AppIconLoader(context: Context) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val densityDpi = context.resources.displayMetrics.densityDpi
    private val sizePx = (72f * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    private val cache = LruCache<String, Bitmap>(80)

    fun load(component: ComponentName): Bitmap? {
        val key = component.flattenToString()
        synchronized(cache) {
            cache.get(key)?.let { return it }
        }
        val drawable = loadDrawable(component) ?: return null
        val bitmap = render(drawable)
        synchronized(cache) {
            cache.put(key, bitmap)
        }
        return bitmap
    }

    private fun loadDrawable(component: ComponentName): Drawable? {
        return try {
            val activities = launcherApps.getActivityList(component.packageName, Process.myUserHandle())
            val info = activities.firstOrNull { it.componentName == component } ?: activities.firstOrNull()
            info?.getIcon(densityDpi)
        } catch (_: SecurityException) {
            null
        }
    }

    private fun render(drawable: Drawable): Bitmap {
        val source = drawable.mutate()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        source.setBounds(0, 0, sizePx, sizePx)
        source.draw(canvas)
        return bitmap
    }
}
