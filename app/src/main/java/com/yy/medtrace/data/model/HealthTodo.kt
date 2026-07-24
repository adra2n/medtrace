package com.yy.medtrace.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "health_todos")
data class HealthTodo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long = 0,
    val memberName: String = "",
    val content: String,
    val dueDate: LocalDate,
    val done: Boolean = false,
    val notifiedDate: String = "",
    val repeatType: String = "none",
    val repeatInterval: Int = 1,
    val startDate: LocalDate = LocalDate.now(),
    val durationDays: Int = 0,
    val completedDates: String = "",
    val category: String = "其他"  // 服药/复查/检查/其他
) {
    fun getStreak(): Int {
        if (completedDates.isBlank()) return 0
        val dates = completedDates.split(",").mapNotNull { 
            try { LocalDate.parse(it.trim()) } catch (e: Exception) { null }
        }.sorted()
        if (dates.isEmpty()) return 0
        
        var streak = 1
        for (i in dates.size - 1 downTo 1) {
            if (dates[i].minusDays(1) == dates[i-1]) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
    
    fun getProgress(): Float {
        if (durationDays <= 0) return 0f
        val completedCount = completedDates.split(",").filter { it.isNotBlank() }.size
        return (completedCount.toFloat() / durationDays).coerceIn(0f, 1f)
    }
}
