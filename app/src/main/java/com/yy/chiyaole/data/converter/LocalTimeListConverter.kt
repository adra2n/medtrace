package com.yy.chiyaole.data.converter

import androidx.room.TypeConverter
import java.time.LocalTime
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocalTimeListConverter {
    private val gson = Gson()
    
    @TypeConverter
    fun fromLocalTimeList(value: List<LocalTime>?): String? {
        return value?.map { it.toString() }?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toLocalTimeList(value: String?): List<LocalTime>? {
        val type = object : TypeToken<List<String>>() {}.type
        return value?.let {
            gson.fromJson<List<String>>(it, type)?.map { timeStr ->
                LocalTime.parse(timeStr)
            }
        }
    }
}
