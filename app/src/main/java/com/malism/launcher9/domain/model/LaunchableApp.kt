package com.malism.launcher9.domain.model

import android.content.ComponentName

data class LaunchableApp(
    val packageName: String,
    val component: ComponentName,
    val label: String,
)
