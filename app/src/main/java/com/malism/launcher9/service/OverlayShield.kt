package com.malism.launcher9.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowManager

object OverlayShield {
    private var view: View? = null

    fun show(context: Context) {
        if (view != null) return
        if (!Settings.canDrawOverlays(context)) return
        val appContext = context.applicationContext
        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val shield = View(appContext).apply {
            setBackgroundColor(0xF2000000.toInt())
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        try {
            windowManager.addView(shield, params)
            view = shield
        } catch (_: RuntimeException) {
            view = null
        }
    }

    fun hide(context: Context) {
        val shield = view ?: return
        val windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            windowManager.removeView(shield)
        } catch (_: RuntimeException) {
            // Already detached.
        } finally {
            view = null
        }
    }
}
