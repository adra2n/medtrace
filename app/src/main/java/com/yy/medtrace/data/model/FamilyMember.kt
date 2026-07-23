package com.yy.medtrace.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relation: String = "",
    val gender: String = "",
    val birthday: String = "",
    val bloodType: String = "",
    val allergy: String = "",
    val chronic: String = "",
    val medicationNote: String = "",
    val otherNote: String = "",
    val isDefault: Boolean = false,
    val avatarPath: String = ""
) {
    companion object {
        val DEFAULT = FamilyMember(name = "我自己", relation = "本人", isDefault = true)
    }
}
