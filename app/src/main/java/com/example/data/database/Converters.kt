package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.DecayFormula
import com.example.data.model.RecurrenceType

class Converters {
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType?): String {
        return value?.name ?: RecurrenceType.DAILY.name
    }

    @TypeConverter
    fun toRecurrenceType(value: String?): RecurrenceType {
        return try {
            if (value != null) RecurrenceType.valueOf(value) else RecurrenceType.DAILY
        } catch (e: Exception) {
            RecurrenceType.DAILY
        }
    }

    @TypeConverter
    fun fromDecayFormula(value: DecayFormula?): String {
        return value?.name ?: DecayFormula.EXPONENTIAL.name
    }

    @TypeConverter
    fun toDecayFormula(value: String?): DecayFormula {
        return try {
            if (value != null) DecayFormula.valueOf(value) else DecayFormula.EXPONENTIAL
        } catch (e: Exception) {
            DecayFormula.EXPONENTIAL
        }
    }
}
