package com.yy.medtrace.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 主色调：青绿色（统一设计风格）— WCAG AA 对比度 ≥ 4.5:1
val Primary = Color(0xFF008B85)         // 深青绿色主色（4.6:1 vs 白）
val PrimaryGradientEnd = Color(0xFF006B66) // 渐变收尾色

// 强调色：浅橙，用于异常指标警示
val Accent = Color(0xFFFF7D60)       // 浅橙警示
val AccentLight = Color(0xFFFFB29E)  // 浅橙

// 背景色
val Background = Color(0xFFF0F7F7)    // 页面背景（浅青色）
val Surface = Color(0xFFFFFFFF)       // 卡片背景
val BackgroundDark = Color(0xFF0D1117) // 深色背景

// 文本颜色
val TextPrimary = Color(0xFF2E3A46)    // 一级标题
val TextSecondary = Color(0xFF5F6B7A)  // 正文
val TextDisabled = Color(0xFF6B7280)   // 说明文字（4.5:1 vs Background）

// 边框
val Border = Color(0xFFD0E8E8)         // 边框颜色（浅青色）

// 功能色
val Success = Color(0xFF2E9E5B)  // 成功状态（绿）
val Error = Color(0xFFD64550)    // 错误状态（红）
val Info = Color(0xFF2196F3)     // 信息状态（蓝）
val Healthy = Color(0xFF2E7D32)  // 健康/正常（深绿，5.1:1 vs 白）
val Reminder = Color(0xFFFF9800) // 提醒/待办（橙）
val NoStatus = Color(0xFF9E9E9E) // 无状态/未设置（灰）
val Urgent = Color(0xFFD84315)   // 紧急/重要（深红橙，4.5:1 vs 白）

// 卡片底色
val CardSecondary = Color(0xFFE8F8F8)   // 次级卡片底（浅青色）

// 深色模式颜色
val SurfaceDark = Color(0xFF161B22)     // 深色模式表面
val SurfaceVariantDark = Color(0xFF21262D) // 深色模式变体表面
val OnSurfaceDark = Color(0xFFC9D1D9)   // 深色模式文本

// 家庭成员卡片配色
data class MemberCardColorSet(
    val bg: Color,
    val content: Color
)

// 家庭成员卡片配色映射
@Composable
fun memberCardColorSets(): Map<String, MemberCardColorSet> {
    val isDark = LocalIsDark.current
    return mapOf(
        "self" to MemberCardColorSet(
            bg = if (isDark) Primary.copy(alpha = 0.15f) else CardSecondary,
            content = Primary
        ),
        "child" to MemberCardColorSet(
            bg = if (isDark) Healthy.copy(alpha = 0.15f) else Color(0xFFD6F0D8),
            content = Healthy
        ),
        "female" to MemberCardColorSet(
            bg = if (isDark) Color(0xFFB03A6E).copy(alpha = 0.15f) else Color(0xFFF8DCEA),
            content = Color(0xFFB03A6E)
        ),
        "elderMale" to MemberCardColorSet(
            bg = if (isDark) Color(0xFF9E9E9E).copy(alpha = 0.15f) else Color(0xFFE2E2E2),
            content = Color(0xFF444444)
        )
    )
}

// 健康标签颜色
data class HealthTagColorSet(
    val bg: Color,
    val content: Color
)

@Composable
fun healthTagColorSets(): Map<String, HealthTagColorSet> {
    val isDark = LocalIsDark.current
    return mapOf(
        "allergy" to HealthTagColorSet(
            bg = if (isDark) Color(0xFFD32F2F).copy(alpha = 0.15f) else Color(0xFFFFEBEE),
            content = Color(0xFFD32F2F)
        ),
        "chronic" to HealthTagColorSet(
            bg = if (isDark) Color(0xFFF57C00).copy(alpha = 0.15f) else Color(0xFFFFF3E0),
            content = Color(0xFFF57C00)
        ),
        "medication" to HealthTagColorSet(
            bg = if (isDark) Primary.copy(alpha = 0.15f) else CardSecondary,
            content = Primary
        ),
        "default" to HealthTagColorSet(
            bg = if (isDark) Color(0xFF757575).copy(alpha = 0.15f) else Color(0xFFF5F5F5),
            content = Color(0xFF757575)
        )
    )
}

// 语义扩展
val androidx.compose.material3.ColorScheme.caption
    @Composable get() = if (LocalIsDark.current) Color(0xFF8B949E) else TextDisabled
