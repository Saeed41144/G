package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RecurrenceType(val displayNameFa: String, val displayNameEn: String) {
    NONE("یک‌باره", "One-time"),
    DAILY("روزانه", "Daily"),
    WEEKLY("هفتگی", "Weekly"),
    MONTHLY("ماهانه", "Monthly")
}

enum class DecayFormula(val displayNameFa: String, val descriptionFa: String) {
    EXPONENTIAL("کاهش نمایی (Exponential)", "کاهش تدریجی و پیوسته با نیمه‌عمر مشخص (طبیعی‌ترین مدل یادگیری)"),
    HYPERBOLIC("کاهش معکوس (Hyperbolic)", "افت سریع‌تر در ابتدا و شیب بسیار ملایم‌تر در دفعات بالا"),
    POWER_LAW("قانون توان (Power Law)", "مدل استاندارد روان‌شناسی مهارت (Logarithmic Learning Curve)")
}

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = SkillEntity::class,
            parentColumns = ["id"],
            childColumns = ["skillId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["skillId"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: Long,
    val title: String,
    val description: String = "",
    val baseGainPercent: Double = 0.01, // e.g. 0.01 = 0.01%
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val completionCount: Int = 0,
    val streakCount: Int = 0,
    val lastCompletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Per-task customization of Diminishing Returns
    val useCustomDecay: Boolean = false,
    val customDecayFormula: DecayFormula = DecayFormula.EXPONENTIAL,
    val customDecayRate: Double = 0.03, // lambda (slope intensity)
    val customMinEfficiency: Double = 0.10 // 0.10 = 10% minimum floor
)
