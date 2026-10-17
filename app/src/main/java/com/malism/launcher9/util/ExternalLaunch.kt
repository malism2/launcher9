package com.malism.launcher9.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.malism.launcher9.data.repository.ParentalRepository

object ExternalLaunch {
    fun start(
        context: Context,
        intent: Intent?,
        parental: ParentalRepository,
        allowWhateverOpens: Boolean,
    ): Boolean {
        if (intent == null) return false
        val resolved = intent.resolveActivity(context.packageManager) ?: return false
        if (allowWhateverOpens) {
            parental.beginExternalSession()
        } else {
            parental.allowTemporary(resolved.packageName)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            if (allowWhateverOpens) parental.cancelExternalSession()
            false
        } catch (_: SecurityException) {
            if (allowWhateverOpens) parental.cancelExternalSession()
            false
        }
    }
}
