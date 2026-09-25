package com.example.domain

import com.example.data.model.AppSettingsEntity
import com.example.data.model.DecayFormula
import com.example.data.model.TaskEntity
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

data class CurvePoint(
    val repetition: Int,
    val marginalGain: Double,
    val efficiencyRatio: Double, // 0.0 to 1.0 (e.g. 0.85 = 85%)
    val cumulativeGain: Double,
    val noDecayCumulativeGain: Double
)

data class DecayConfig(
    val isEnabled: Boolean,
    val formula: DecayFormula,
    val decayRate: Double,
    val minEfficiency: Double
)

object DecayCalculator {

    /**
     * Resolves the active decay configuration for a task based on whether
     * the task overrides global settings or uses default app settings.
     */
    fun resolveConfig(task: TaskEntity, settings: AppSettingsEntity?): DecayConfig {
        val globalEnabled = settings?.decayEnabled ?: true
        val globalFormula = settings?.defaultFormula ?: DecayFormula.EXPONENTIAL
        val globalRate = settings?.defaultDecayRate ?: 0.025
        val globalFloor = settings?.defaultMinEfficiency ?: 0.10

        return if (task.useCustomDecay) {
            DecayConfig(
                isEnabled = globalEnabled, // Global toggle still respects enabling/disabling
                formula = task.customDecayFormula,
                decayRate = task.customDecayRate,
                minEfficiency = task.customMinEfficiency
            )
        } else {
            DecayConfig(
                isEnabled = globalEnabled,
                formula = globalFormula,
                decayRate = globalRate,
                minEfficiency = globalFloor
            )
        }
    }

    /**
     * Calculates the efficiency ratio (0.0 to 1.0) for a given repetition count.
     * repetition: 0 means the very first time doing the task (100% efficiency).
     */
    fun calculateEfficiency(
        repetition: Int,
        config: DecayConfig
    ): Double {
        if (!config.isEnabled || repetition <= 0) return 1.0

        val floor = config.minEfficiency.coerceIn(0.0, 0.99)
        val rate = config.decayRate.coerceAtLeast(0.001)

        val rawDecay = when (config.formula) {
            DecayFormula.EXPONENTIAL -> exp(-rate * repetition)
            DecayFormula.HYPERBOLIC -> 1.0 / (1.0 + rate * repetition)
            DecayFormula.POWER_LAW -> (repetition + 1.0).pow(-rate * 2.5)
        }

        val clampedDecay = rawDecay.coerceIn(0.0, 1.0)
        return floor + (1.0 - floor) * clampedDecay
    }

    /**
     * Calculates the effective percentage gain for the upcoming repetition.
     */
    fun calculateEffectiveGain(
        baseGain: Double,
        repetition: Int,
        config: DecayConfig
    ): Double {
        val efficiency = calculateEfficiency(repetition, config)
        return baseGain * efficiency
    }

    /**
     * Calculates the effective gain directly from a task and app settings.
     */
    fun calculateNextGain(task: TaskEntity, settings: AppSettingsEntity?): Double {
        val config = resolveConfig(task, settings)
        return calculateEffectiveGain(task.baseGainPercent, task.completionCount, config)
    }

    /**
     * Generates curve points for plotting and simulation in the chart.
     */
    fun generateCurvePoints(
        baseGain: Double,
        config: DecayConfig,
        maxReps: Int = 80
    ): List<CurvePoint> {
        val points = ArrayList<CurvePoint>(maxReps + 1)
        var cumulative = 0.0
        var noDecayCumulative = 0.0

        for (rep in 0..maxReps) {
            val efficiency = calculateEfficiency(rep, config)
            val marginal = baseGain * efficiency
            cumulative += marginal
            noDecayCumulative += baseGain

            points.add(
                CurvePoint(
                    repetition = rep,
                    marginalGain = marginal,
                    efficiencyRatio = efficiency,
                    cumulativeGain = cumulative,
                    noDecayCumulativeGain = noDecayCumulative
                )
            )
        }
        return points
    }

    /**
     * Estimates the half-life: the number of completions at which efficiency
     * drops by 50% between 100% and the minimum floor.
     */
    fun calculateHalfLifeReps(config: DecayConfig): Int {
        if (!config.isEnabled || config.decayRate <= 0) return 0
        return when (config.formula) {
            DecayFormula.EXPONENTIAL -> (ln(2.0) / config.decayRate).roundToInt().coerceAtLeast(1)
            DecayFormula.HYPERBOLIC -> (1.0 / config.decayRate).roundToInt().coerceAtLeast(1)
            DecayFormula.POWER_LAW -> (2.0.pow(1.0 / (config.decayRate * 2.5)) - 1.0).roundToInt().coerceAtLeast(1)
        }
    }

    /**
     * Nicely formats a small decimal percentage string (e.g. +0.010% or +0.0075%).
     */
    fun formatGainPercent(gain: Double, includePlus: Boolean = true): String {
        val prefix = if (includePlus) "+" else ""
        return if (gain < 0.01) {
            prefix + String.format(java.util.Locale.US, "%.4f%%", gain)
        } else if (gain < 0.1) {
            prefix + String.format(java.util.Locale.US, "%.3f%%", gain)
        } else {
            prefix + String.format(java.util.Locale.US, "%.2f%%", gain)
        }
    }

    /**
     * Formats mastery percent e.g. "24.58%"
     */
    fun formatMasteryPercent(mastery: Double): String {
        return String.format(java.util.Locale.US, "%.2f%%", mastery)
    }

    /**
     * Formats efficiency percentage e.g. "82%"
     */
    fun formatEfficiency(ratio: Double): String {
        return "${(ratio * 100).roundToInt()}%"
    }
}
