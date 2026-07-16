package com.yy.chiyaole.data.model

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
    val isDefault: Boolean = false
)
