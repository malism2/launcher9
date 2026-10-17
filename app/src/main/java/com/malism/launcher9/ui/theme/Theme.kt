package com.malism.launcher9.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat

private val Colors = lightColorScheme(
    primary = Color(0xFF1B4F72),
    onPrimary = Color.White,
    secondary = Color(0xFF8E1B1B),
    onSecondary = Color.White,
    background = Color(0xFFF4F1EA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFF4F1EA),
    onSurface = Color(0xFF1A1A1A),
    error = Color(0xFF8E1B1B),
    onError = Color.White,
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun Launcher9Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = AppTypography,
        content = content,
    )
}

@Composable
fun SystemBarIcons(lightBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as Activity).window
        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = lightBackground
        controller.isAppearanceLightNavigationBars = lightBackground
    }
}
