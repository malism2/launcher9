package com.malism.launcher9.ui.navigation

import android.net.Uri

object Routes {
    const val Home = "home"
    const val Picker = "picker"
    const val Vault = "vault"
    const val Settings = "settings"
    const val Auth = "auth/{purpose}?target={target}"

    fun auth(purpose: String, target: String = ""): String {
        return "auth/$purpose?target=${Uri.encode(target)}"
    }
}
