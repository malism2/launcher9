package com.malism.launcher9.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malism.launcher9.LauncherApp
import com.malism.launcher9.R
import com.malism.launcher9.ui.AppViewModelFactory
import com.malism.launcher9.ui.components.LargeButton
import com.malism.launcher9.ui.components.PinPad
import com.malism.launcher9.ui.theme.SystemBarIcons
import com.malism.launcher9.util.ExternalLaunch
import com.malism.launcher9.util.SystemIntents

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as LauncherApp).container
    val viewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory(container))
    val biometricOn by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val homeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refresh()
    }
    SystemBarIcons(lightBackground = true)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        if (viewModel.pinStep == PinChangeStep.Idle) {
            Text(
                settingsMessage(viewModel.message),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            LargeButton(
                text = stringResource(R.string.settings_pin),
                onClick = viewModel::startPinChange,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (viewModel.biometricAvailable) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_bio),
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = biometricOn,
                        onCheckedChange = viewModel::setBiometricEnabled,
                    )
                }
            }
            Text(
                stringResource(R.string.settings_permissions),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 28.dp),
            )
            PermissionRow(
                label = stringResource(R.string.setup_usage_title),
                granted = viewModel.usageGranted,
                action = stringResource(R.string.setup_usage_action),
                onClick = {
                    ExternalLaunch.start(
                        context,
                        SystemIntents.usageAccess(),
                        container.parentalRepository,
                        allowWhateverOpens = true,
                    )
                },
            )
            PermissionRow(
                label = stringResource(R.string.setup_overlay_title),
                granted = viewModel.overlayGranted,
                action = stringResource(R.string.setup_overlay_action),
                onClick = {
                    ExternalLaunch.start(
                        context,
                        SystemIntents.overlay(context),
                        container.parentalRepository,
                        allowWhateverOpens = true,
                    )
                },
            )
            Text(
                stringResource(R.string.settings_default_home),
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                stringResource(
                    if (viewModel.defaultHome) R.string.setup_home_on else R.string.setup_home_off,
                ),
                fontSize = 18.sp,
            )
            LargeButton(
                text = stringResource(R.string.settings_set_home),
                onClick = {
                    container.parentalRepository.beginExternalSession()
                    homeLauncher.launch(SystemIntents.requestHomeRole(context))
                },
                modifier = Modifier.padding(top = 12.dp),
            )
            LargeButton(
                text = stringResource(R.string.settings_wallpaper),
                onClick = {
                    val started = ExternalLaunch.start(
                        context = context,
                        intent = SystemIntents.wallpaper(),
                        parental = container.parentalRepository,
                        allowWhateverOpens = true,
                    )
                    if (!started) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.wallpaper_unavailable),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Text(
                pinStepTitle(viewModel.pinStep),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            PinPad(
                pin = viewModel.draft,
                error = settingsMessage(viewModel.message).ifBlank { null },
                enabled = true,
                showBiometric = false,
                submitLabel = stringResource(R.string.ok),
                biometricLabel = "",
                deleteLabel = stringResource(R.string.delete),
                onDigit = viewModel::append,
                onDelete = viewModel::delete,
                onSubmit = viewModel::submitPinStep,
                onBiometric = {},
                modifier = Modifier.padding(top = 8.dp),
            )
            LargeButton(
                text = stringResource(R.string.cancel),
                onClick = viewModel::cancelPinChange,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun pinStepTitle(step: PinChangeStep): String = when (step) {
    PinChangeStep.Current -> stringResource(R.string.settings_pin_current)
    PinChangeStep.New -> stringResource(R.string.settings_pin_new)
    PinChangeStep.Confirm -> stringResource(R.string.settings_pin_confirm)
    PinChangeStep.Idle -> ""
}

@Composable
private fun settingsMessage(message: SettingsMessage?): String = when (message) {
    null -> ""
    SettingsMessage.Wrong -> stringResource(R.string.pin_wrong)
    SettingsMessage.NotSet -> stringResource(R.string.pin_not_set)
    SettingsMessage.Short -> stringResource(R.string.setup_pin_short)
    SettingsMessage.Mismatch -> stringResource(R.string.setup_pin_mismatch)
    SettingsMessage.Saved -> stringResource(R.string.settings_pin_saved)
    is SettingsMessage.Locked -> stringResource(R.string.pin_locked, message.seconds)
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    Text(label, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
    Text(
        stringResource(if (granted) R.string.permission_on else R.string.permission_off),
        fontSize = 18.sp,
    )
    if (!granted) {
        LargeButton(text = action, onClick = onClick, modifier = Modifier.padding(top = 8.dp))
    }
}
