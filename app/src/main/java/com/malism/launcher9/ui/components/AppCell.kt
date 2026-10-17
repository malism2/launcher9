package com.malism.launcher9.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malism.launcher9.domain.model.LaunchableApp
import com.malism.launcher9.ui.icon.AppIcon

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCell(
    app: LaunchableApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
    onWallpaper: Boolean = false,
    iconSize: Dp = 72.dp,
    labelSize: TextUnit = 18.sp,
    interactionEnabled: Boolean = true,
) {
    val labelColor = if (onWallpaper) Color.White else Color(0xFF1A1A1A)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0x331B4F72) else Color.Transparent)
            .then(
                if (interactionEnabled) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(component = app.component, label = app.label, size = iconSize)
        Text(
            text = app.label,
            color = labelColor,
            fontSize = labelSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = wallpaperLabelShadow(onWallpaper)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddCell(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    labelSize: TextUnit = 12.sp,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(Color(0xCC1B4F72), RoundedCornerShape(iconSize / 4)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "＋", color = Color.White, fontSize = 28.sp)
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = labelSize,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = wallpaperLabelShadow(true)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

private fun wallpaperLabelShadow(enabled: Boolean): Shadow? {
    if (!enabled) return null
    return Shadow(
        color = Color(0x99000000),
        offset = Offset(0f, 1f),
        blurRadius = 4f,
    )
}
