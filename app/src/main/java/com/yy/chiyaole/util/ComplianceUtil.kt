package com.yy.chiyaole.util

import com.yy.chiyaole.data.model.MedicationRecord
import com.yy.chiyaole.data.model.MedicationStatus
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 用药依从率计算工具类
 */
object ComplianceUtil {
    /**
     * 计算指定时间段内的依从率
     * @param records 服药记录列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 依从率（0-1之间的浮点数）
     */
    fun calculateComplianceRate(
        records: List<MedicationRecord>,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): Float {
        val periodRecords = records.filter { record ->
            record.scheduledTime.isAfter(startTime) && 
            record.scheduledTime.isBefore(endTime)
        }
        
        if (periodRecords.isEmpty()) return 1.0f
        
        val takenCount = periodRecords.count { it.status == MedicationStatus.TAKEN }
        return takenCount.toFloat() / periodRecords.size
    }

    /**
     * 计算每日依从率
     */
    fun calculateDailyComplianceRate(
        records: List<MedicationRecord>,
        date: LocalDateTime = LocalDateTime.now()
    ): Float {
        val startOfDay = date.truncatedTo(ChronoUnit.DAYS)
        val endOfDay = startOfDay.plusDays(1)
        return calculateComplianceRate(records, startOfDay, endOfDay)
    }

    /**
     * 计算每周依从率
     */
    fun calculateWeeklyComplianceRate(
        records: List<MedicationRecord>,
        date: LocalDateTime = LocalDateTime.now()
    ): Float {
        val endOfWeek = date.truncatedTo(ChronoUnit.DAYS)
        val startOfWeek = endOfWeek.minusDays(7)
        return calculateComplianceRate(records, startOfWeek, endOfWeek)
    }

    /**
     * 计算每月依从率
     */
    fun calculateMonthlyComplianceRate(
        records: List<MedicationRecord>,
        date: LocalDateTime = LocalDateTime.now()
    ): Float {
        val endOfMonth = date.truncatedTo(ChronoUnit.DAYS)
        val startOfMonth = endOfMonth.minusDays(30)
        return calculateComplianceRate(records, startOfMonth, endOfMonth)
    }

    /**
     * 获取依从率等级
     */
    fun getComplianceLevel(rate: Float): ComplianceLevel {
        return when {
            rate >= 0.9f -> ComplianceLevel.EXCELLENT
            rate >= 0.8f -> ComplianceLevel.GOOD
            rate >= 0.6f -> ComplianceLevel.FAIR
            else -> ComplianceLevel.POOR
        }
    }

    /**
     * 获取依从率评价和建议
     */
    fun getComplianceAdvice(rate: Float): String {
        return when (getComplianceLevel(rate)) {
            ComplianceLevel.EXCELLENT -> "太棒了！继续保持这样的用药习惯"
            ComplianceLevel.GOOD -> "做得不错！稍加注意就能做得更好"
            ComplianceLevel.FAIR -> "还需要改进，建议设置提醒以免忘记服药"
            ComplianceLevel.POOR -> "请务必重视按时服药，这关系到治疗效果"
        }
    }
}

enum class ComplianceLevel {
    EXCELLENT,  // 优秀
    GOOD,       // 良好
    FAIR,       // 一般
    POOR        // 差
}
