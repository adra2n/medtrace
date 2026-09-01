package com.yy.medtrace.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "health_todos",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["memberId"]),
        Index(value = ["dueDate", "done"])
    ]
)
data class HealthTodo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: Long = 0,
    val memberName: String = "",
    val content: String,
    val dueDate: LocalDate,
    val done: Boolean = false,
    val repeatType: String = "none",  // none、day、week
    val category: String = "其他",    // 吃药、其他
    val reminderTime: String = "09:00"  // HH:mm 格式，默认 9 点
)
