package com.malism.launcher9.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import com.malism.launcher9.domain.model.LaunchableApp

object DefaultHomeApps {
    fun resolve(context: Context, installed: List<LaunchableApp>): List<LaunchableApp> {
        if (installed.isEmpty()) return emptyList()
        val byPackage = installed.associateBy { it.packageName }
        val packages = listOfNotNull(
            dialerPackage(context),
            smsPackage(context),
            contactsPackage(context),
            chromePackage(context, byPackage.keys),
            settingsPackage(context, byPackage.keys),
        ).distinct()
        return packages.mapNotNull { byPackage[it] }
    }

    private fun dialerPackage(context: Context): String? {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val dialer = telecom?.defaultDialerPackage
        if (!dialer.isNullOrBlank()) return dialer
        return resolvePackage(context, Intent(Intent.ACTION_DIAL))
    }

    private fun smsPackage(context: Context): String? {
        return Telephony.Sms.getDefaultSmsPackage(context)
            ?: resolvePackage(
                context,
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            )
    }

    private fun contactsPackage(context: Context): String? {
        return resolvePackage(
            context,
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CONTACTS),
        )
    }

    private fun chromePackage(context: Context, installed: Set<String>): String? {
        val chromeCandidates = listOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
        )
        chromeCandidates.firstOrNull { it in installed }?.let { return it }
        return resolvePackage(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")),
        )
    }

    private fun settingsPackage(context: Context, installed: Set<String>): String? {
        resolvePackage(context, Intent(Settings.ACTION_SETTINGS))?.let { return it }
        return "com.android.settings".takeIf { it in installed }
    }

    private fun resolvePackage(context: Context, intent: Intent): String? {
        val resolve = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ) ?: return null
        val packageName = resolve.activityInfo?.packageName ?: return null
        if (packageName == "android") return null
        return packageName
    }
}
