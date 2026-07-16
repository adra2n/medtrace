package com.yy.chiyaole.data.backup

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.yy.chiyaole.data.model.FamilyMember
import com.yy.chiyaole.data.model.MedicalRecord
import com.yy.chiyaole.data.model.UserSettings
import java.time.LocalDateTime

data class BackupData(
    val version: Int = 1,
    val members: List<FamilyMember> = emptyList(),
    val records: List<MedicalRecord> = emptyList(),
    val settings: UserSettings? = null
)

private val localDateTimeAdapter = object : TypeAdapter<LocalDateTime>() {
    override fun write(out: JsonWriter, value: LocalDateTime?) {
        out.value(value?.toString())
    }

    override fun read(`in`: JsonReader): LocalDateTime? {
        val raw = `in`.nextString()
        return runCatching { LocalDateTime.parse(raw) }.getOrNull()
    }
}

private val gson = GsonBuilder()
    .registerTypeAdapter(LocalDateTime::class.java, localDateTimeAdapter)
    .setPrettyPrinting()
    .create()

fun encodeBackup(data: BackupData): String = gson.toJson(data)

fun decodeBackup(json: String): BackupData =
    gson.fromJson(json, BackupData::class.java)
