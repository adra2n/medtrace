package com.yy.chiyaole.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.yy.chiyaole.data.AppDatabase

val LocalIsDark = compositionLocalOf { false }

@Composable
fun cardContainerColor(): Color = if (LocalIsDark.current) CardSurfaceDark else CardSurface

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    secondary = AccentLight,
    tertiary = PrimaryDark,
    background = Color(0xFF0F1A1B),
    surface = Color(0xFF1A2729),
    onPrimary = Color(0xFF06201F),
    onSecondary = Color(0xFF1A1206),
    onTertiary = Color(0xFFD7F2F0),
    onBackground = Color(0xFFE2EAEA),
    onSurface = Color(0xFFE2EAEA),
    error = Error,
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Accent,
    tertiary = Warning,
    background = Background,
    surface = Surface,
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFF000000),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(LocalIsDark provides useDarkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}