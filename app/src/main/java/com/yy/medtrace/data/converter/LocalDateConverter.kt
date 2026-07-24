package com.yy.medtrace.data.converter

import androidx.room.TypeConverter
import java.time.LocalDate

class LocalDateConverter {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value)
        } catch (e: Exception) {
            null
        }
    }
}
