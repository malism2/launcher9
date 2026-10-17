package com.malism.launcher9.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.malism.launcher9.ui.block.BlockActivity

object BlockServiceController {
    fun start(context: Context) {
        val intent = Intent(context, AppBlockService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun openBlock(context: Context, packageName: String) {
        val label = appLabel(context, packageName)
        val intent = Intent(context, BlockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockActivity.EXTRA_PACKAGE, packageName)
            putExtra(BlockActivity.EXTRA_LABEL, label)
        }
        try {
            context.startActivity(intent)
        } catch (_: RuntimeException) {
            BlockUi.visible = false
        }
    }

    private fun appLabel(context: Context, packageName: String): String {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
