package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val decayEnabled: Boolean = true,
    val defaultFormula: DecayFormula = DecayFormula.EXPONENTIAL,
    val defaultDecayRate: Double = 0.025, // default slope
    val defaultMinEfficiency: Double = 0.10, // 10% floor
    val language: String = "FA", // "FA" or "EN"
    val hapticFeedback: Boolean = true
)
