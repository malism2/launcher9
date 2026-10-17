package com.malism.launcher9.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malism.launcher9.LauncherApp
import com.malism.launcher9.R
import com.malism.launcher9.ui.AppViewModelFactory
import com.malism.launcher9.ui.components.PinPad
import com.malism.launcher9.ui.theme.SystemBarIcons
import com.malism.launcher9.util.DeviceState

@Composable
fun AuthScreen(
    purpose: String,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as LauncherApp).container
    val viewModel: AuthViewModel = viewModel(
        key = purpose,
        factory = AppViewModelFactory(container),
    )
    val biometricSaved by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val hardwareReady = DeviceState.biometricAuthenticators(context) != null
    SystemBarIcons(lightBackground = true)

    val title = when (purpose) {
        "add" -> stringResource(R.string.auth_add)
        "drawer", "vault" -> stringResource(R.string.auth_drawer)
        "wallpaper" -> stringResource(R.string.auth_wallpaper)
        "settings" -> stringResource(R.string.auth_settings)
        "remove" -> stringResource(R.string.auth_remove)
        else -> stringResource(R.string.auth_settings)
    }
    val error = when (val current = viewModel.error) {
        null -> null
        AuthError.Wrong -> stringResource(R.string.pin_wrong)
        AuthError.NotSet -> stringResource(R.string.pin_not_set)
        is AuthError.Locked -> stringResource(R.string.pin_locked, current.seconds)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F1EA))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF1A1A1A))
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
            onSubmit = { viewModel.submit(onSuccess) },
            onBiometric = {
                showBiometricPrompt(
                    activity = context as FragmentActivity,
                    onSuccess = onSuccess,
                    onError = {},
                )
            },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
