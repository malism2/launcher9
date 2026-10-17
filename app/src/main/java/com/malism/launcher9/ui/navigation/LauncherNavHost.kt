package com.malism.launcher9.ui.navigation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.malism.launcher9.LauncherApp
import com.malism.launcher9.R
import com.malism.launcher9.ui.auth.AuthScreen
import com.malism.launcher9.ui.home.HomeScreen
import com.malism.launcher9.ui.picker.AppPickerScreen
import com.malism.launcher9.ui.settings.SettingsScreen
import com.malism.launcher9.ui.vault.VaultScreen
import com.malism.launcher9.util.ExternalLaunch
import com.malism.launcher9.util.SystemIntents
import kotlinx.coroutines.launch

@Composable
fun LauncherNavHost(onRegister: (NavHostController) -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val parental = (context.applicationContext as LauncherApp).container.parentalRepository
    LaunchedEffect(navController) { onRegister(navController) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val atHome = backStackEntry?.destination?.route == Routes.Home
    // On home: absorb Back so the launcher doesn't keep exiting the task.
    // Elsewhere: jump straight home instead of stepping through the stack.
    BackHandler {
        if (!atHome) {
            navController.popBackStack(Routes.Home, inclusive = false)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    onAdd = { navController.navigate(Routes.auth("add")) },
                    onAllApps = { navController.navigate(Routes.auth("drawer")) },
                    onSettings = { navController.navigate(Routes.auth("settings")) },
                    onRemove = { app -> navController.navigate(Routes.auth("remove", app.packageName)) },
                )
            }
            composable(
                route = Routes.Auth,
                arguments = listOf(
                    navArgument("purpose") { type = NavType.StringType },
                    navArgument("target") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val purpose = entry.arguments?.getString("purpose").orEmpty()
                val target = entry.arguments?.getString("target").orEmpty()
                AuthScreen(purpose = purpose) {
                    when (purpose) {
                        "add" -> navController.navigate(Routes.Picker) {
                            popUpTo(Routes.Home)
                        }
                        "drawer", "vault" -> navController.navigate(Routes.Vault) {
                            popUpTo(Routes.Home)
                        }
                        "settings" -> navController.navigate(Routes.Settings) {
                            popUpTo(Routes.Home)
                        }
                        "wallpaper" -> {
                            val started = ExternalLaunch.start(
                                context = context,
                                intent = SystemIntents.wallpaper(),
                                parental = parental,
                                allowWhateverOpens = true,
                            )
                            if (!started) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.wallpaper_unavailable),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            navController.popBackStack(Routes.Home, inclusive = false)
                        }
                        "remove" -> {
                            val app = context.applicationContext as LauncherApp
                            app.container.appScope.launch {
                                if (target.isNotBlank()) parental.removeFromHome(target)
                            }
                            navController.popBackStack(Routes.Home, inclusive = false)
                        }
                        else -> navController.popBackStack(Routes.Home, inclusive = false)
                    }
                }
            }
            composable(Routes.Picker) {
                Box(Modifier.fillMaxSize().background(Color(0xFFF4F1EA))) {
                    AppPickerScreen(onDone = { navController.popBackStack(Routes.Home, inclusive = false) })
                }
            }
            composable(Routes.Vault) {
                Box(Modifier.fillMaxSize().background(Color(0xFFF4F1EA))) {
                    VaultScreen()
                }
            }
            composable(Routes.Settings) {
                Box(Modifier.fillMaxSize().background(Color(0xFFF4F1EA))) {
                    SettingsScreen()
                }
            }
        }
    }
}
