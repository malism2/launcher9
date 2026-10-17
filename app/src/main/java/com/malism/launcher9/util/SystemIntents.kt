package com.malism.launcher9.util

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings

object SystemIntents {
    fun requestHomeRole(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun usageAccess(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun overlay(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    )

    fun wallpaper(): Intent = Intent(Intent.ACTION_SET_WALLPAPER)

    fun dial(context: Context): Intent? {
        val dial = Intent(Intent.ACTION_DIAL)
        return if (dial.resolveActivity(context.packageManager) != null) dial else null
    }

    fun camera(context: Context): Intent? {
        val candidates = listOf(
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        for (intent in candidates) {
            if (intent.resolveActivity(context.packageManager) != null) return intent
        }
        return null
    }

    fun appDetails(packageName: String): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:$packageName"),
    )

    fun emergency(context: Context): Intent? {
        val packageManager = context.packageManager
        val candidates = listOf(
            Intent("com.android.phone.EmergencyDialer.DIAL"),
            Intent(Intent.ACTION_MAIN).setComponent(
                ComponentName("com.android.phone", "com.android.phone.EmergencyDialer"),
            ),
            Intent(Intent.ACTION_MAIN).setComponent(
                ComponentName(
                    "com.google.android.dialer",
                    "com.android.dialer.emergency.EmergencyDialerActivity",
                ),
            ),
            Intent(Intent.ACTION_MAIN).setComponent(
                ComponentName(
                    "com.samsung.android.dialer",
                    "com.samsung.android.dialer.emergency.EmergencyDialerActivity",
                ),
            ),
        )
        for (intent in candidates) {
            if (intent.resolveActivity(packageManager) != null) return intent
        }
        val dial = Intent(Intent.ACTION_DIAL)
        return if (dial.resolveActivity(packageManager) != null) dial else null
    }
}
