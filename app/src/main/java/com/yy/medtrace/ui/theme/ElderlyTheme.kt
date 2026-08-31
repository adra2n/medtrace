package com.yy.medtrace.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 适老化主题配置
 * 专为老年用户设计，包含大字体、大按钮、高对比度
 */
object ElderlyTheme {
    // 字体大小
    val fontSizeSmall = 16.sp
    val fontSizeMedium = 18.sp
    val fontSizeLarge = 20.sp
    val fontSizeExtraLarge = 24.sp
    
    // 按钮尺寸
    val buttonMinHeight = 60.dp
    val buttonMinWidth = 48.dp
    val buttonPadding = 16.dp
    
    // 间距
    val spacingSmall = 8.dp
    val spacingMedium = 12.dp
    val spacingLarge = 16.dp
    val spacingExtraLarge = 24.dp
    
    // 圆角
    val cornerRadius = 12.dp
    
    // 颜色对比度
    val minContrastRatio = 4.5f
}

/**
 * 适老化样式扩展
 */
val ElderlyButtonMinHeight = ElderlyTheme.buttonMinHeight
val ElderlyButtonMinWidth = ElderlyTheme.buttonMinWidth
val ElderlyButtonPadding = ElderlyTheme.buttonPadding

val ElderlyFontSizeSmall = ElderlyTheme.fontSizeSmall
val ElderlyFontSizeMedium = ElderlyTheme.fontSizeMedium
val ElderlyFontSizeLarge = ElderlyTheme.fontSizeLarge
val ElderlyFontSizeExtraLarge = ElderlyTheme.fontSizeExtraLarge

val ElderlySpacingSmall = ElderlyTheme.spacingSmall
val ElderlySpacingMedium = ElderlyTheme.spacingMedium
val ElderlySpacingLarge = ElderlyTheme.spacingLarge
val ElderlySpacingExtraLarge = ElderlyTheme.spacingExtraLarge

val ElderlyCornerRadius = ElderlyTheme.cornerRadius
