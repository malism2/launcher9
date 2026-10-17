package com.malism.launcher9.ui.setup

import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malism.launcher9.LauncherApp
import com.malism.launcher9.R
import com.malism.launcher9.ui.AppViewModelFactory
import com.malism.launcher9.ui.components.LargeButton
import com.malism.launcher9.ui.components.PinPad
import com.malism.launcher9.ui.theme.SystemBarIcons
import com.malism.launcher9.util.SystemIntents

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as LauncherApp).container
    val viewModel: SetupViewModel = viewModel(factory = AppViewModelFactory(container))
    val lifecycleOwner = LocalLifecycleOwner.current
    val homeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refresh()
    }
    SystemBarIcons(lightBackground = true)
    BackHandler(enabled = viewModel.step != SetupStep.Welcome) { viewModel.back() }

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
        when (viewModel.step) {
            SetupStep.Welcome -> {
                Text(stringResource(R.string.setup_welcome_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.setup_welcome_body),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            SetupStep.HomeRole -> {
                Text(stringResource(R.string.setup_home_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.setup_home_body),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(if (viewModel.isDefaultHome) R.string.setup_home_on else R.string.setup_home_off),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
                LargeButton(
                    text = stringResource(R.string.setup_home_action),
                    onClick = { homeLauncher.launch(SystemIntents.requestHomeRole(context)) },
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            SetupStep.Pin, SetupStep.PinConfirm -> {
                val confirming = viewModel.step == SetupStep.PinConfirm
                Text(
                    stringResource(if (confirming) R.string.setup_pin_confirm_title else R.string.setup_pin_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (!confirming) {
                    Text(
                        stringResource(R.string.setup_pin_body),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                val error = when (viewModel.error) {
                    null -> null
                    SetupError.Short -> stringResource(R.string.setup_pin_short)
                    SetupError.Mismatch -> stringResource(R.string.setup_pin_mismatch)
                }
                PinPad(
                    pin = if (confirming) viewModel.confirm else viewModel.pin,
                    error = error,
                    enabled = true,
                    showBiometric = false,
                    submitLabel = stringResource(R.string.next),
                    biometricLabel = "",
                    deleteLabel = stringResource(R.string.delete),
                    onDigit = viewModel::append,
                    onDelete = viewModel::delete,
                    onSubmit = { viewModel.next {} },
                    onBiometric = {},
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            SetupStep.Biometric -> {
                Text(stringResource(R.string.setup_bio_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(
                        if (viewModel.biometricAvailable) R.string.setup_bio_body else R.string.setup_bio_unavailable,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                if (viewModel.biometricAvailable) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.setup_bio_switch),
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = viewModel.biometricOn,
                            onCheckedChange = { viewModel.biometricOn = it },
                        )
                    }
                }
            }
            SetupStep.Usage -> {
                Text(stringResource(R.string.setup_usage_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.setup_usage_body),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(if (viewModel.usageGranted) R.string.permission_on else R.string.permission_off),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
                LargeButton(
                    text = stringResource(R.string.setup_usage_action),
                    onClick = { context.startActivity(SystemIntents.usageAccess()) },
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(R.string.setup_skip_hint),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            SetupStep.Overlay -> {
                Text(stringResource(R.string.setup_overlay_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.setup_overlay_body),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(if (viewModel.overlayGranted) R.string.permission_on else R.string.permission_off),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
                LargeButton(
                    text = stringResource(R.string.setup_overlay_action),
                    onClick = { context.startActivity(SystemIntents.overlay(context)) },
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(R.string.setup_skip_hint),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        if (viewModel.step != SetupStep.Pin && viewModel.step != SetupStep.PinConfirm) {
            LargeButton(
                text = stringResource(
                    if (viewModel.step == SetupStep.Overlay) R.string.setup_done else R.string.next,
                ),
                onClick = { viewModel.next {} },
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        if (viewModel.step != SetupStep.Welcome &&
            viewModel.step != SetupStep.Pin &&
            viewModel.step != SetupStep.PinConfirm
        ) {
            LargeButton(
                text = stringResource(R.string.back),
                onClick = viewModel::back,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
