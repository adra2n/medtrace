package com.yy.medtrace.ui.theme

import androidx.compose.ui.graphics.Color

// 根据家庭成员关系/性别推导卡片配色（ui.md 成员卡片配色映射）
// 🟡本人 / 🟢儿童 / 🌸女性 / ⚫长辈男性
fun memberCardColors(relation: String, gender: String): Pair<Color, Color> {
    val rel = relation.trim()
    val g = gender.trim()
    return when {
        rel.contains("本人") || rel.contains("自己") || rel.contains("我") ->
            MemberColors.SelfBg to MemberColors.SelfContent
        rel.contains("子") || rel.contains("儿") || rel.contains("宝") || rel.contains("婴") ->
            MemberColors.ChildBg to MemberColors.ChildContent
        g == "女" || rel.contains("妈") || rel.contains("妻") || rel.contains("姐") || rel.contains("妹") || rel.contains("女") ->
            MemberColors.FemaleBg to MemberColors.FemaleContent
        g == "男" ->
            MemberColors.ElderMaleBg to MemberColors.ElderMaleContent
        else -> MemberColors.FemaleBg to MemberColors.FemaleContent
    }
}

fun computeAge(birthday: String): Int? {
    if (birthday.isBlank()) return null
    runCatching {
        val parts = birthday.split("-", "/", ".").mapNotNull { it.toIntOrNull() }
        if (parts.size < 3) return null
        val (y, m, d) = parts
        val today = java.time.LocalDate.now()
        var age = today.year - y
        if (today.monthValue < m || (today.monthValue == m && today.dayOfMonth < d)) age--
        return age
    }
    return null
}
