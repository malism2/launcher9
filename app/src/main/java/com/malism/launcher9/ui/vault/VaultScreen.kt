package com.malism.launcher9.ui.vault

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.malism.launcher9.LauncherApp
import com.malism.launcher9.R
import com.malism.launcher9.domain.model.LaunchableApp
import com.malism.launcher9.ui.AppViewModelFactory
import com.malism.launcher9.ui.components.AppCell
import com.malism.launcher9.ui.theme.SystemBarIcons
import com.malism.launcher9.util.ExternalLaunch
import com.malism.launcher9.util.SystemIntents

@Composable
fun VaultScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as LauncherApp).container
    val viewModel: VaultViewModel = viewModel(factory = AppViewModelFactory(container))
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var menuApp by remember { mutableStateOf<LaunchableApp?>(null) }
    SystemBarIcons(lightBackground = true)

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F1EA))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        Text(stringResource(R.string.drawer_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.drawer_hint),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )
        if (apps.isEmpty()) {
            Text(
                stringResource(R.string.drawer_empty),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 24.dp, start = 4.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(apps, key = { it.component.flattenToString() }) { app ->
                    Box {
                        AppCell(
                            app = app,
                            iconSize = 56.dp,
                            labelSize = 12.sp,
                            onClick = { viewModel.open(app) },
                            onLongClick = { menuApp = app },
                        )
                        DropdownMenu(
                            expanded = menuApp?.packageName == app.packageName,
                            onDismissRequest = { menuApp = null },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.app_info), fontSize = 18.sp) },
                                onClick = {
                                    menuApp = null
                                    val started = ExternalLaunch.start(
                                        context = context,
                                        intent = SystemIntents.appDetails(app.packageName),
                                        parental = container.parentalRepository,
                                        allowWhateverOpens = true,
                                    )
                                    if (!started) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.app_info_unavailable),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
