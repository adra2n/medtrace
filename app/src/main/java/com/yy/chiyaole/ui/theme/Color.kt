package com.yy.chiyaole.ui.theme

import androidx.compose.ui.graphics.Color

// 主色调：柔和青蓝，传达健康、安心（ui.md 规范）
val Primary = Color(0xFF36A3C7)      // 主青蓝
val PrimaryLight = Color(0xFF7FC4DC) // 浅青蓝
val PrimaryDark = Color(0xFF1E7D9B)  // 深青蓝（渐变顶栏末端）
val PrimaryGradientEnd = Color(0xFF1E7D9B) // 渐变顶栏收尾色

// 强调色：浅橙，用于异常指标警示（ui.md 规范）
val Accent = Color(0xFFFF7D60)       // 浅橙警示
val AccentLight = Color(0xFFFFB29E)  // 浅橙
val AccentDark = Color(0xFFE85C3C)   // 深橙

// 背景色：纯白浅米，柔和通透（ui.md 规范）
val Background = Color(0xFFFAF8F4)    // 纯白浅米
val Surface = Color(0xFFFFFFFF)       // 纯白
val BackgroundDark = Color(0xFF101B1C) // 深冷灰背景

// 文本颜色
val TextPrimary = Color(0xFF1F2D32)    // 主要文本
val TextSecondary = Color(0xFF5A6B70)  // 次要文本
val TextDisabled = Color(0xFFA7B2B5)   // 禁用文本

// 功能色
val Success = Color(0xFF2E9E5B)  // 成功状态（绿）
val Error = Color(0xFFD64550)    // 错误状态（红）
val Warning = Color(0xFFFF7D60)  // 警告状态（浅橙）

// 卡片底色
val CardSurface = Color(0xFFF0F7FA)     // 浅色模式卡片底
val CardSurfaceDark = Color(0xFF1B2A2C) // 深色模式卡片底

// 家庭成员卡片配色映射（ui.md 规范）
object MemberColors {
    // 本人
    val SelfBg = Color(0xFFE3F2F8)
    val SelfContent = Color(0xFF1E7D9B)
    // 儿童
    val ChildBg = Color(0xFFD6F0D8)
    val ChildContent = Color(0xFF2E7D32)
    // 女性
    val FemaleBg = Color(0xFFF8DCEA)
    val FemaleContent = Color(0xFFB03A6E)
    // 长辈男性
    val ElderMaleBg = Color(0xFFE2E2E2)
    val ElderMaleContent = Color(0xFF444444)
}
