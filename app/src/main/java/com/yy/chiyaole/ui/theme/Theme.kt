package com.yy.chiyaole.ui.theme

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
import com.yy.chiyaole.data.AppDatabase

val LocalIsDark = compositionLocalOf { false }

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun cardContainerColor(): Color = if (LocalIsDark.current) CardSurfaceDark else CardSurface

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color(0xFF00302F),
    primaryContainer = Color(0xFF0E4A49),
    onPrimaryContainer = Color(0xFFB2E8E6),
    secondary = AccentLight,
    onSecondary = Color(0xFF3A2406),
    secondaryContainer = Color(0xFF5A3E16),
    onSecondaryContainer = Color(0xFFF2D6AE),
    tertiary = PrimaryLight,
    onTertiary = Color(0xFF00302F),
    tertiaryContainer = Color(0xFF15403F),
    onTertiaryContainer = Color(0xFFBDEAE8),
    background = BackgroundDark,
    onBackground = Color(0xFFE2EAEA),
    surface = Color(0xFF162324),
    onSurface = Color(0xFFE2EAEA),
    surfaceVariant = Color(0xFF2A3A3C),
    onSurfaceVariant = Color(0xFFB6C6C6),
    outline = Color(0xFF3D4F51),
    outlineVariant = Color(0xFF2C3A3C),
    error = Error,
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEDEB),
    onPrimaryContainer = Color(0xFF003734),
    secondary = Accent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4E2CC),
    onSecondaryContainer = Color(0xFF3A2406),
    tertiary = Primary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCDEDEB),
    onTertiaryContainer = Color(0xFF003734),
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE2EBEA),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFFC2D2D1),
    outlineVariant = Color(0xFFD8E3E2),
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
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val settings by database.userSettingsDao().getUserSettings().collectAsState(initial = null)
    val useDarkTheme = darkTheme ?: settings?.darkMode ?: isSystemInDarkTheme()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
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
