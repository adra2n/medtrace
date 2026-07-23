package com.yy.medtrace.ui.theme

import androidx.compose.ui.graphics.Color

// 主色调：亮蓝（ui.md 规范）
val Primary = Color(0xFF3BA9F5)        // Primary Button/Active Icon/Active Tab/CTA/Link
val PrimaryLight = Color(0xFFDDF3FF)   // Icon Background/Selected Background/Light Card
val PrimaryDark = Color(0xFF2A8FD8)    // 深蓝（渐变末端，保证顶栏白字可读）
val PrimaryGradientEnd = Color(0xFF2A8FD8) // 渐变收尾色
val PrimarySoft = Color(0xFF6CCFF6)    // 次要浅蓝（辅助）

// 强调色：浅橙，用于异常指标警示
val Accent = Color(0xFFFF7D60)       // 浅橙警示
val AccentLight = Color(0xFFFFB29E)  // 浅橙
val AccentDark = Color(0xFFE85C3C)   // 深橙

// 背景色（ui.md 规范）
val Background = Color(0xFFF8FAFC)    // 页面背景
val Surface = Color(0xFFFFFFFF)       // 卡片背景
val BackgroundDark = Color(0xFF0D1117) // GitHub 风格深色背景（更护眼）

// 模块区分底色（统一用 PrimaryLight 浅蓝）
val BgFamily = Color(0xFFDDF3FF)      // 家人模块
val BgTodo = Color(0xFFDDF3FF)        // 待办模块
val BgQuick = Color(0xFFDDF3FF)       // 快捷功能模块

// 文本颜色（ui.md 规范）
val TextPrimary = Color(0xFF2E3A46)    // 一级标题
val TextSecondary = Color(0xFF5F6B7A)  // 正文
val TextDisabled = Color(0xFF98A2B3)   // 说明文字

// 边框
val Border = Color(0xFFE6EEF5)         // 边框颜色

// 功能色
val Success = Color(0xFF2E9E5B)  // 成功状态（绿）
val Error = Color(0xFFD64550)    // 错误状态（红）
val Warning = Color(0xFFFF7D60)  // 警告状态（浅橙）

// 卡片底色
val CardSurface = Color(0xFFFFFFFF)     // 卡片：纯白
val CardSecondary = Color(0xFFDDF3FF)   // 次级卡片底（Primary Light）
val CardSurfaceDark = Color(0xFF161B22) // 深色模式卡片底（GitHub 风格）

// 深色模式增强颜色
val SurfaceDark = Color(0xFF161B22)     // 深色模式表面
val SurfaceVariantDark = Color(0xFF21262D) // 深色模式变体表面
val OnSurfaceDark = Color(0xFFC9D1D9)   // 深色模式文本

// 家庭成员卡片配色映射
object MemberColors {
    // 本人
    val SelfBg = Color(0xFFDDF3FF)
    val SelfContent = Color(0xFF2A8FD8)
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

// 语义扩展（方便直接用 MaterialTheme.colorScheme.caption）
val androidx.compose.material3.ColorScheme.caption
    get() = TextDisabled
