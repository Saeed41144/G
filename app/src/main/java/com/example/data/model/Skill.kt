package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "عمومی", // e.g. کدنویسی, زبان, هنر, ورزش, دانش
    val colorHex: String = "#6366F1",
    val iconName: String = "bolt",
    val currentMastery: Double = 0.0, // 0.0 to 100.0%
    val targetMastery: Double = 100.0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val levelName: String
        get() = when {
            currentMastery >= 90.0 -> "استاد (Master)"
            currentMastery >= 75.0 -> "متخصص (Expert)"
            currentMastery >= 50.0 -> "پیشرفته (Proficient)"
            currentMastery >= 25.0 -> "ماهر (Competent)"
            currentMastery >= 10.0 -> "کارآموز (Apprentice)"
            else -> "تازه‌کار (Novice)"
        }

    val levelTier: Int
        get() = when {
            currentMastery >= 90.0 -> 5
            currentMastery >= 75.0 -> 4
            currentMastery >= 50.0 -> 3
            currentMastery >= 25.0 -> 2
            currentMastery >= 10.0 -> 1
            else -> 0
        }
}
