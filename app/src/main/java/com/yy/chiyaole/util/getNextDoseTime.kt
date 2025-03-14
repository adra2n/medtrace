package com.yy.chiyaole.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.yy.chiyaole.data.model.MedicationReminder
import java.time.LocalDateTime

// 计算下一次服药时间的函数
@RequiresApi(Build.VERSION_CODES.O)
fun getNextDoseTime(reminder: MedicationReminder): LocalDateTime? {
    val now = LocalDateTime.now()
    if (!reminder.isActive || now.isAfter(reminder.endDate) || reminder.medicationTimes.isEmpty()) {
        return null
    }

    // 如果当前时间在开始日期之前，返回开始日期的第一个服药时间
    if (now.isBefore(reminder.startDate)) {
        return reminder.startDate.with(reminder.medicationTimes[0])
    }

    val currentTime = now.toLocalTime()
    val today = now.toLocalDate()

    // 找到今天的下一个服药时间
    val nextTimeToday = reminder.medicationTimes.find { it.isAfter(currentTime) }

    return if (nextTimeToday != null) {
        // 今天还有服药时间
        now.with(nextTimeToday)
    } else {
        // 今天没有剩余的服药时间，返回明天的第一个服药时间
        now.plusDays(1).with(reminder.medicationTimes[0])
    }.let { nextTime ->
        // 检查是否超过结束日期
        if (nextTime.isAfter(reminder.endDate)) null else nextTime
    }
}