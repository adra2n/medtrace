package com.yy.chiyaole.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yy.chiyaole.data.model.MedicationItem

class MedicationItemListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromMedicationItemList(list: List<MedicationItem>?): String = gson.toJson(list ?: emptyList<MedicationItem>())

    @TypeConverter
    fun toMedicationItemList(value: String?): List<MedicationItem> {
        val type = object : TypeToken<List<MedicationItem>>() {}.type
        return value?.let { gson.fromJson(it, type) } ?: emptyList()
    }
}
