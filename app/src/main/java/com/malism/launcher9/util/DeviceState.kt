package com.malism.launcher9.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.biometric.BiometricManager

object DeviceState {
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolve?.activityInfo?.packageName == context.packageName
    }

    fun biometricAuthenticators(context: Context): Int? {
        val manager = BiometricManager.from(context)
        val strong = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val weak = BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when {
            manager.canAuthenticate(strong) == BiometricManager.BIOMETRIC_SUCCESS -> strong
            manager.canAuthenticate(weak) == BiometricManager.BIOMETRIC_SUCCESS -> weak
            else -> null
        }
    }

    fun isExemptPackage(packageName: String, selfPackage: String): Boolean {
        if (packageName == selfPackage) return true
        if (packageName == "android") return true
        if (packageName == "com.android.systemui") return true
        if (packageName == "com.android.permissioncontroller") return true
        if (packageName == "com.google.android.permissioncontroller") return true
        if (packageName == "com.android.server.telecom") return true
        if (packageName.contains("incall", ignoreCase = true)) return true
        return false
    }
}
