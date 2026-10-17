package com.malism.launcher9.ui.block

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malism.launcher9.LauncherApp
import com.malism.launcher9.R
import com.malism.launcher9.service.BlockUi
import com.malism.launcher9.service.OverlayShield
import com.malism.launcher9.ui.AppViewModelFactory
import com.malism.launcher9.ui.auth.AuthError
import com.malism.launcher9.ui.auth.AuthViewModel
import com.malism.launcher9.ui.auth.showBiometricPrompt
import com.malism.launcher9.ui.components.LargeButton
import com.malism.launcher9.ui.components.PinPad
import com.malism.launcher9.ui.theme.Launcher9Theme
import com.malism.launcher9.ui.theme.SystemBarIcons
import com.malism.launcher9.util.DeviceState

class BlockActivity : FragmentActivity() {
    private var blockedPackage by mutableStateOf("")
    private var blockedLabel by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        readIntent(intent)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = goHome()
            },
        )
        enableEdgeToEdge()
        val container = (application as LauncherApp).container
        setContent {
            val viewModel: AuthViewModel = viewModel(factory = AppViewModelFactory(container))
            val biometricSaved by viewModel.biometricEnabled.collectAsStateWithLifecycle()
            val hardwareReady = DeviceState.biometricAuthenticators(this) != null
            val error = when (val current = viewModel.error) {
                null -> null
                AuthError.Wrong -> stringResource(R.string.pin_wrong)
                AuthError.NotSet -> stringResource(R.string.pin_not_set)
                is AuthError.Locked -> stringResource(R.string.pin_locked, current.seconds)
            }
            Launcher9Theme {
                SystemBarIcons(lightBackground = true)
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                ) {
                    Text(blockedLabel, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF1A1A1A))
                    Text(
                        stringResource(R.string.block_message),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )
                    PinPad(
                        pin = viewModel.pin,
                        error = error,
                        enabled = !viewModel.locked,
                        showBiometric = biometricSaved && hardwareReady,
                        submitLabel = stringResource(R.string.ok),
                        biometricLabel = stringResource(R.string.biometric_action),
                        deleteLabel = stringResource(R.string.delete),
                        onDigit = viewModel::append,
                        onDelete = viewModel::delete,
                        onSubmit = { viewModel.submit { unlock() } },
                        onBiometric = {
                            showBiometricPrompt(
                                activity = this@BlockActivity,
                                onSuccess = { unlock() },
                                onError = {},
                            )
                        },
                    )
                    LargeButton(
                        text = stringResource(R.string.block_home),
                        onClick = { goHome() },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        BlockUi.visible = true
        OverlayShield.hide(applicationContext)
    }

    override fun onStop() {
        BlockUi.visible = false
        super.onStop()
    }

    private fun readIntent(intent: Intent?) {
        blockedPackage = intent?.getStringExtra(EXTRA_PACKAGE).orEmpty()
        blockedLabel = intent?.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { blockedPackage }
    }

    private fun unlock() {
        if (blockedPackage.isNotBlank()) {
            (application as LauncherApp).container.parentalRepository.allowSession(blockedPackage)
        }
        OverlayShield.hide(applicationContext)
        finish()
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(home)
        finish()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    companion object {
        const val EXTRA_PACKAGE = "blocked_package"
        const val EXTRA_LABEL = "blocked_label"
    }
}
