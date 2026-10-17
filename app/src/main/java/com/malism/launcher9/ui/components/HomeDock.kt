package com.malism.launcher9.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.malism.launcher9.R

@Composable
fun HomeDock(
    onSettings: () -> Unit,
    onCamera: () -> Unit,
    onAllApps: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
) {
    Row(modifier.fillMaxWidth()) {
        DockItem(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.action_settings),
            onClick = onSettings,
            iconSize = iconSize,
            modifier = Modifier.weight(1f),
        )
        DockItem(
            icon = Icons.Filled.PhotoCamera,
            label = stringResource(R.string.action_camera),
            onClick = onCamera,
            iconSize = iconSize,
            modifier = Modifier.weight(1f),
        )
        DockItem(
            icon = Icons.Filled.Apps,
            label = stringResource(R.string.action_all_apps),
            onClick = onAllApps,
            iconSize = iconSize,
            modifier = Modifier.weight(1f),
        )
        DockItem(
            icon = Icons.Filled.Add,
            label = stringResource(R.string.action_add),
            onClick = onAdd,
            iconSize = iconSize,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xCC1B4F72),
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconSize / 4))
                .background(containerColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(iconSize * 0.5f),
            )
        }
    }
}
