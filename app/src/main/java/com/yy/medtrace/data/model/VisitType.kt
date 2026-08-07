package com.yy.medtrace.data.model

enum class VisitType(val label: String) {
    OUTPATIENT("门诊"),
    EMERGENCY("急诊"),
    CHECKUP("体检"),
    FOLLOW_UP("复查"),
    SELF_MED("自购药"),
    OTHER("其他");

    companion object {
        private val labelMap = entries.associateBy { it.label }

        fun fromLabel(label: String): VisitType =
            labelMap[label] ?: OTHER

        val labels: List<String> = entries.map { it.label }
    }
}
