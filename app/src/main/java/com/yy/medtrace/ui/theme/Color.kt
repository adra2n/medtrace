package com.yy.medtrace.ui.theme

import androidx.compose.ui.graphics.Color

// 主色调：青绿色（统一设计风格）
val Primary = Color(0xFF2ECDC6)        // 青绿色主色
val PrimaryLight = Color(0xFFE8F8F8)   // 浅青色背景
val PrimaryDark = Color(0xFF25B5B0)    // 深青色
val PrimaryGradientEnd = Color(0xFF25B5B0) // 渐变收尾色
val PrimarySoft = Color(0xFF7DD8D3)    // 次要浅青色

// 强调色：浅橙，用于异常指标警示
val Accent = Color(0xFFFF7D60)       // 浅橙警示
val AccentLight = Color(0xFFFFB29E)  // 浅橙
val AccentDark = Color(0xFFE85C3C)   // 深橙

// 背景色
val Background = Color(0xFFF0F7F7)    // 页面背景（浅青色）
val Surface = Color(0xFFFFFFFF)       // 卡片背景
val BackgroundDark = Color(0xFF0D1117) // 深色背景

// 模块区分底色
val BgFamily = Color(0xFFE8F8F8)      // 家人模块
val BgTodo = Color(0xFFE8F8F8)        // 待办模块
val BgQuick = Color(0xFFE8F8F8)       // 快捷功能模块

// 文本颜色
val TextPrimary = Color(0xFF2E3A46)    // 一级标题
val TextSecondary = Color(0xFF5F6B7A)  // 正文
val TextDisabled = Color(0xFF98A2B3)   // 说明文字

// 边框
val Border = Color(0xFFD0E8E8)         // 边框颜色（浅青色）

// 功能色
val Success = Color(0xFF2E9E5B)  // 成功状态（绿）
val Error = Color(0xFFD64550)    // 错误状态（红）
val Warning = Color(0xFFFF7D60)  // 警告状态（浅橙）

// 卡片底色
val CardSurface = Color(0xFFFFFFFF)     // 卡片：纯白
val CardSecondary = Color(0xFFE8F8F8)   // 次级卡片底（浅青色）
val CardSurfaceDark = Color(0xFF161B22) // 深色模式卡片底

// 深色模式增强颜色
val SurfaceDark = Color(0xFF161B22)     // 深色模式表面
val SurfaceVariantDark = Color(0xFF21262D) // 深色模式变体表面
val OnSurfaceDark = Color(0xFFC9D1D9)   // 深色模式文本

// 家庭成员卡片配色映射
object MemberColors {
    val SelfBg = Color(0xFFE8F8F8)
    val SelfContent = Color(0xFF2ECDC6)
    val ChildBg = Color(0xFFD6F0D8)
    val ChildContent = Color(0xFF2E7D32)
    val FemaleBg = Color(0xFFF8DCEA)
    val FemaleContent = Color(0xFFB03A6E)
    val ElderMaleBg = Color(0xFFE2E2E2)
    val ElderMaleContent = Color(0xFF444444)
}

// 健康标签颜色
object HealthTagColors {
    val AllergyBg = Color(0xFFFFEBEE)
    val AllergyContent = Color(0xFFD32F2F)
    val ChronicBg = Color(0xFFFFF3E0)
    val ChronicContent = Color(0xFFF57C00)
    val MedicationBg = Color(0xFFE8F8F8)
    val MedicationContent = Color(0xFF2ECDC6)
    val DefaultBg = Color(0xFFF5F5F5)
    val DefaultContent = Color(0xFF757575)
}

// 语义扩展
val androidx.compose.material3.ColorScheme.caption
    get() = TextDisabled
