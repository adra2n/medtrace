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
    val repeatInterval: Int = 1
)
