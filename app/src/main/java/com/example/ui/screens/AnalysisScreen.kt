package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.DecayFormula
import com.example.data.model.SkillEntity
import com.example.data.model.TaskEntity
import com.example.domain.DecayCalculator
import com.example.domain.DecayConfig
import com.example.ui.components.DecayCurveChart
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary

@Composable
fun AnalysisScreen(
    tasks: List<TaskEntity>,
    skills: List<SkillEntity>,
    settings: AppSettingsEntity?,
    selectedTaskId: Long?,
    onUpdateTaskCustomDecay: (TaskEntity, DecayFormula, Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current task being inspected or null for simulation sandbox
    var currentTaskId by remember { mutableStateOf(selectedTaskId) }

    LaunchedEffect(selectedTaskId) {
        if (selectedTaskId != null) {
            currentTaskId = selectedTaskId
        }
    }

    val selectedTask = remember(tasks, currentTaskId) {
        tasks.find { it.id == currentTaskId }
    }

    // Parameters for simulator
    var formula by remember { mutableStateOf(DecayFormula.EXPONENTIAL) }
    var decayRate by remember { mutableDoubleStateOf(0.025) }
    var minFloor by remember { mutableDoubleStateOf(0.10) }
    var baseGain by remember { mutableDoubleStateOf(0.01) }

    // When selectedTask changes, sync parameters
    LaunchedEffect(selectedTask, settings) {
        if (selectedTask != null) {
            val config = DecayCalculator.resolveConfig(selectedTask, settings)
            formula = config.formula
            decayRate = config.decayRate
            minFloor = config.minEfficiency
            baseGain = selectedTask.baseGainPercent
        } else if (settings != null) {
            formula = settings.defaultFormula
            decayRate = settings.defaultDecayRate
            minFloor = settings.defaultMinEfficiency
            baseGain = 0.01
        }
    }

    val currentConfig = remember(formula, decayRate, minFloor, settings) {
        DecayConfig(
            isEnabled = settings?.decayEnabled ?: true,
            formula = formula,
            decayRate = decayRate,
            minEfficiency = minFloor
        )
    }

    val halfLife = remember(currentConfig) {
        DecayCalculator.calculateHalfLifeReps(currentConfig)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "تحلیل و کنترل شیب کاهش بازدهی",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "مشاهده زنده افت تدریجی اثر تسک‌ها و کنترل پارامترهای منحنی",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Task Picker Horizontal Carousel
        item {
            Column {
                Text(
                    text = "انتخاب تسک برای تحلیل:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = currentTaskId == null,
                            onClick = { currentTaskId = null },
                            label = { Text("شبیه‌ساز آزاد", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_sandbox_mode")
                        )
                    }

                    items(tasks) { task ->
                        val isSelected = task.id == currentTaskId
                        FilterChip(
                            selected = isSelected,
                            onClick = { currentTaskId = task.id },
                            label = { Text(task.title, fontSize = 12.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_analysis_task_${task.id}")
                        )
                    }
                }
            }
        }

        // Active Interactive Canvas Chart
        item {
            DecayCurveChart(
                baseGain = baseGain,
                config = currentConfig,
                currentRepetition = selectedTask?.completionCount ?: 0,
                maxReps = 75,
                showControls = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sliders & Curve Controls Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "کنترل و تنظیم شیب منحنی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedTask != null) {
                            Button(
                                onClick = {
                                    onUpdateTaskCustomDecay(selectedTask, formula, decayRate, minFloor)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_save_curve_to_task")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اعمال روی این تسک", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Formula Selector
                    Text(
                        text = "مدل ریاضیاتی کاهش:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DecayFormula.values().forEach { f ->
                            FilterChip(
                                selected = formula == f,
                                onClick = { formula = f },
                                label = { Text(f.name.replace("_", " "), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Slope / Decay Rate Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "شدت شیب کاهش (Decay Rate):",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.3f", decayRate) +
                                    if (decayRate < 0.02) " (ملایم)" else if (decayRate > 0.05) " (بسیار تند)" else " (متعادل)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }

                    Slider(
                        value = decayRate.toFloat(),
                        onValueChange = { decayRate = it.toDouble() },
                        valueRange = 0.005f..0.080f,
                        modifier = Modifier.fillMaxWidth().testTag("slider_analysis_decay_rate")
                    )

                    // Minimum Efficiency Floor Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "کف حداقل اثر حفظ‌شده:",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${(minFloor * 100).toInt()}% از اثر اولیه",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                    }

                    Slider(
                        value = minFloor.toFloat(),
                        onValueChange = { minFloor = it.toDouble() },
                        valueRange = 0.05f..0.45f,
                        modifier = Modifier.fillMaxWidth().testTag("slider_analysis_min_floor")
                    )
                }
            }
        }

        // Scientific Insights & Milestone Forecast
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بینش تحلیلی و شبیه‌سازی آینده",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricPill(
                            title = "نقطه نیمه‌عمر",
                            value = "تکرار $halfLife-ام",
                            subtitle = "افت ۵۰٪ راندمان",
                            color = IndigoPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val gainAt25 = DecayCalculator.calculateEffectiveGain(baseGain, 25, currentConfig)
                        MetricPill(
                            title = "اثر در تکرار ۲۵",
                            value = DecayCalculator.formatGainPercent(gainAt25),
                            subtitle = "بازدهی ${(DecayCalculator.calculateEfficiency(25, currentConfig) * 100).toInt()}%",
                            color = EmeraldTertiary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val gainAt50 = DecayCalculator.calculateEffectiveGain(baseGain, 50, currentConfig)
                        MetricPill(
                            title = "اثر در تکرار ۵۰",
                            value = DecayCalculator.formatGainPercent(gainAt50),
                            subtitle = "کف ${(minFloor * 100).toInt()}%",
                            color = AmberAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Educational notice
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "قانون بازده نزولی (Law of Diminishing Returns) بیان می‌کند که انجام یک تسک یکنواخت به مرور زمان اثر یادگیری کمتری ایجاد می‌کند. برای حفظ شتاب رشد در تسلط بر مهارت، باید پس از گذشت چند هفته تسک‌های چالشی‌تر یا متنوع‌تری تعریف کنید.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
