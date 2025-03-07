package com.yy.chiyaole.data.converter

import androidx.room.TypeConverter
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LocalTimeConverter {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")

    @TypeConverter
    fun fromTimeString(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, formatter) }
    }
    
    @TypeConverter
    fun timeToString(time: LocalTime?): String? {
        return time?.format(formatter)
    }

    @TypeConverter
    fun fromTimeListString(value: String?): List<LocalTime> {
        return value?.split(",")?.map { LocalTime.parse(it.trim(), formatter) } ?: emptyList()
    }

    @TypeConverter
    fun timeListToString(times: List<LocalTime>?): String {
        return times?.joinToString(",") { it.format(formatter) } ?: ""
    }
}
