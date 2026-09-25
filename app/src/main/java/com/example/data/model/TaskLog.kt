package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_logs",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SkillEntity::class,
            parentColumns = ["id"],
            childColumns = ["skillId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["skillId"])
    ]
)
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val skillId: Long,
    val repetitionIndex: Int, // The nth time this task was performed (1, 2, 3...)
    val baseGain: Double, // e.g. 0.01%
    val effectiveGain: Double, // calculated gain after diminishing returns
    val efficiencyRatio: Double, // effectiveGain / baseGain (e.g. 0.85 = 85%)
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
