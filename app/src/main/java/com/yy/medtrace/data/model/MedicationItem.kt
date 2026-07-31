package com.yy.medtrace.data.model

data class MedicationItem(
    val name: String = "",
    val dose: String = "",
    val doseUnit: String = "",
    val freqDays: String = "1",
    val freqTimes: String = "1",
    val freq: String = "",
    val duration: String = ""
)
