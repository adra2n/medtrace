package com.yy.medtrace.data.backup

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.model.HealthTodo
import com.yy.medtrace.data.model.MedicalRecord
import com.yy.medtrace.data.model.UserSettings
import java.time.LocalDate
import java.time.LocalDateTime

data class BackupData(
    val version: Int = 1,
    val schemaVersion: Int = 1,
    val members: List<FamilyMember> = emptyList(),
    val records: List<MedicalRecord> = emptyList(),
    val todos: List<HealthTodo> = emptyList(),
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

private val localDateAdapter = object : TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter, value: LocalDate?) {
        out.value(value?.toString())
    }

    override fun read(`in`: JsonReader): LocalDate? {
        val raw = `in`.nextString()
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }
}

private val gson = GsonBuilder()
    .registerTypeAdapter(LocalDateTime::class.java, localDateTimeAdapter)
    .registerTypeAdapter(LocalDate::class.java, localDateAdapter)
    .setPrettyPrinting()
    .create()

fun encodeBackup(data: BackupData): String = gson.toJson(data)

fun decodeBackup(json: String): BackupData =
    gson.fromJson(json, BackupData::class.java)
