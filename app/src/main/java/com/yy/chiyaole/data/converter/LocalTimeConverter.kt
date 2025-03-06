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
}
