package com.malism.launcher9.ui.icon

import android.content.ComponentName
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.malism.launcher9.LauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIcon(
    component: ComponentName,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val context = LocalContext.current
    val loader = (context.applicationContext as LauncherApp).container.iconLoader
    var bitmap by remember(component) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(component) {
        bitmap = withContext(Dispatchers.IO) { loader.load(component) }
    }
    val corner = RoundedCornerShape(size / 5)
    val image = bitmap
    if (image == null) {
        Box(
            modifier
                .size(size)
                .clip(corner)
                .background(Color(0xFF6B7280)),
        )
    } else {
        val rendered = remember(image) { image.asImageBitmap() }
        Image(
            bitmap = rendered,
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .clip(corner),
        )
    }
}
