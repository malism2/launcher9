package com.malism.launcher9

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.malism.launcher9.domain.model.BootState
import com.malism.launcher9.service.BlockServiceController
import com.malism.launcher9.ui.navigation.LauncherNavHost
import com.malism.launcher9.ui.navigation.Routes
import com.malism.launcher9.ui.theme.Launcher9Theme

class MainActivity : FragmentActivity() {
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LauncherApp).container
        setContent {
            val boot by container.parentalRepository.bootState.collectAsStateWithLifecycle()
            Launcher9Theme {
                when (boot) {
                    BootState.Loading, BootState.NeedSetup -> Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF4F1EA)),
                    )
                    BootState.Ready -> LauncherNavHost(onRegister = { navController = it })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val parental = (application as LauncherApp).container.parentalRepository
        parental.endExternalSession()
        parental.revokeAllSessions()
        if (parental.setupComplete.value) {
            BlockServiceController.start(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            navController?.popBackStack(Routes.Home, inclusive = false)
        }
    }
}
