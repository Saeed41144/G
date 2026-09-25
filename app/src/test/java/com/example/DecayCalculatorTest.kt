package com.example

import com.example.data.model.AppSettingsEntity
import com.example.data.model.DecayFormula
import com.example.data.model.TaskEntity
import com.example.domain.DecayCalculator
import com.example.domain.DecayConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecayCalculatorTest {

    @Test
    fun testFirstRepetitionHasFullEfficiency() {
        val config = DecayConfig(
            isEnabled = true,
            formula = DecayFormula.EXPONENTIAL,
            decayRate = 0.03,
            minEfficiency = 0.10
        )
        val gain = DecayCalculator.calculateEffectiveGain(
            baseGain = 0.01,
            repetition = 0,
            config = config
        )
        assertEquals(0.01, gain, 0.00001)
    }

    @Test
    fun testGainDecreasesWithRepetitions() {
        val config = DecayConfig(
            isEnabled = true,
            formula = DecayFormula.EXPONENTIAL,
            decayRate = 0.03,
            minEfficiency = 0.10
        )
        val gain0 = DecayCalculator.calculateEffectiveGain(0.01, 0, config)
        val gain10 = DecayCalculator.calculateEffectiveGain(0.01, 10, config)
        val gain30 = DecayCalculator.calculateEffectiveGain(0.01, 30, config)
        val gain80 = DecayCalculator.calculateEffectiveGain(0.01, 80, config)

        assertTrue("Gain at rep 10 should be less than rep 0", gain10 < gain0)
        assertTrue("Gain at rep 30 should be less than rep 10", gain30 < gain10)
        assertTrue("Gain at rep 80 should be less than rep 30", gain80 < gain30)
    }

    @Test
    fun testGainDoesNotDropBelowFloor() {
        val minFloor = 0.10
        val baseGain = 0.01
        val config = DecayConfig(
            isEnabled = true,
            formula = DecayFormula.EXPONENTIAL,
            decayRate = 0.05,
            minEfficiency = minFloor
        )
        val gain1000 = DecayCalculator.calculateEffectiveGain(baseGain, 1000, config)
        val expectedFloorGain = baseGain * minFloor

        assertTrue("Gain after many repetitions should not drop below floor", gain1000 >= expectedFloorGain - 0.000001)
    }

    @Test
    fun testDisabledDecayReturnsFullBaseGain() {
        val config = DecayConfig(
            isEnabled = false,
            formula = DecayFormula.EXPONENTIAL,
            decayRate = 0.05,
            minEfficiency = 0.10
        )
        val gain50 = DecayCalculator.calculateEffectiveGain(0.01, 50, config)
        assertEquals(0.01, gain50, 0.00001)
    }

    @Test
    fun testResolveConfigRespectsCustomOverride() {
        val settings = AppSettingsEntity(
            decayEnabled = true,
            defaultFormula = DecayFormula.EXPONENTIAL,
            defaultDecayRate = 0.02,
            defaultMinEfficiency = 0.10
        )
        val taskWithCustom = TaskEntity(
            skillId = 1,
            title = "Test Task",
            useCustomDecay = true,
            customDecayFormula = DecayFormula.HYPERBOLIC,
            customDecayRate = 0.05,
            customMinEfficiency = 0.25
        )
        val resolved = DecayCalculator.resolveConfig(taskWithCustom, settings)

        assertEquals(DecayFormula.HYPERBOLIC, resolved.formula)
        assertEquals(0.05, resolved.decayRate, 0.0001)
        assertEquals(0.25, resolved.minEfficiency, 0.0001)
    }

    @Test
    fun testFormatGainPercent() {
        val formattedSmall = DecayCalculator.formatGainPercent(0.0075)
        assertEquals("+0.0075%", formattedSmall)

        val formattedMedium = DecayCalculator.formatGainPercent(0.01)
        assertEquals("+0.010%", formattedMedium)
    }
}
