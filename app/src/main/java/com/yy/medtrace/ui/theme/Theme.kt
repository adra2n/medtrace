package com.yy.medtrace.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import com.yy.medtrace.data.AppDatabase

val LocalIsDark = compositionLocalOf { false }

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),      // Icon Background
    medium = RoundedCornerShape(16.dp),     // Button
    large = RoundedCornerShape(20.dp),      // Card
    extraLarge = RoundedCornerShape(28.dp)  // Bottom Tab
)

@Composable
fun cardContainerColor(): Color = if (LocalIsDark.current) SurfaceDark else Color.White

@Composable
fun screenBackground(): Color = if (LocalIsDark.current) BackgroundDark else Background

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4), // 浅青绿色（深色模式下可见）
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF004D46),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Accent,
    onSecondary = Color(0xFF3A1500),
    secondaryContainer = Color(0xFF5A2410),
    onSecondaryContainer = Color(0xFFFFD2C4),
    tertiary = Color(0xFF80DEEA),
    onTertiary = Color(0xFF00363A),
    tertiaryContainer = Color(0xFF004D5A),
    onTertiaryContainer = Color(0xFFB2EBF2),
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D),
    outlineVariant = Color(0xFF21262D),
    error = Error,
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6EEF6),
    onPrimaryContainer = Color(0xFF003542),
    secondary = Accent,
    onSecondary = Color(0xFF3A1500), // 浅橙 Accent 上用深色文字，白字仅 ~2.5:1 未达 AA，深棕约 6.5:1
    secondaryContainer = Color(0xFFFFE0D6),
    onSecondaryContainer = Color(0xFF5A1500),
    tertiary = Primary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6EEF6),
    onTertiaryContainer = Color(0xFF003542),
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = CardSecondary,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Color(0xFFEFF4F8),
    error = Error,
    onError = Color(0xFFFFFFFF)
)

@Composable
fun ChiyaoleTheme(
    darkTheme: Boolean? = null,
    // 默认不使用动态颜色，因为我们要保持一致的高对比度配色
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDarkTheme = darkTheme ?: isSystemInDarkTheme()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(LocalIsDark provides useDarkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
