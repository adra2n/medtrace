package com.yy.medtrace.data.model

import java.time.LocalDate

data class MedicationItem(
    val name: String = "",           // 药品名称
    val dosage: String = "",         // 剂量（如：每次1片）
    val frequency: String = "",      // 频次（如：每天3次）
    val usage: String = "",          // 用法（如：饭后服用）
    val startDate: LocalDate? = null, // 开始日期
    val endDate: LocalDate? = null,   // 结束日期（null表示长期服用）
    val notes: String = ""           // 备注
)
