package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.data.model.DecayFormula
import com.example.data.model.RecurrenceType
import com.example.data.model.SkillEntity
import com.example.data.model.TaskEntity
import com.example.domain.DecayCalculator
import com.example.domain.DecayConfig
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoPrimary

private val PRESET_GAINS = listOf(0.01, 0.02, 0.05, 0.1, 0.5, 1.0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskDialog(
    task: TaskEntity? = null,
    defaultSkillId: Long? = null,
    skills: List<SkillEntity>,
    settings: AppSettingsEntity?,
    onDismiss: () -> Unit,
    onConfirm: (
        skillId: Long,
        title: String,
        description: String,
        baseGain: Double,
        recurrence: RecurrenceType,
        useCustomDecay: Boolean,
        decayFormula: DecayFormula,
        decayRate: Double,
        minFloor: Double
    ) -> Unit
) {
    var selectedSkillId by remember {
        mutableStateOf(task?.skillId ?: defaultSkillId ?: skills.firstOrNull()?.id ?: 0L)
    }
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var baseGainText by remember {
        mutableStateOf(
            if (task != null) {
                if (task.baseGainPercent < 0.01) String.format(java.util.Locale.US, "%.4f", task.baseGainPercent)
                else String.format(java.util.Locale.US, "%.3f", task.baseGainPercent).trimEnd('0').trimEnd('.')
            } else "0.01"
        )
    }
    var recurrenceType by remember { mutableStateOf(task?.recurrenceType ?: RecurrenceType.DAILY) }

    // Diminishing returns custom overrides
    var useCustomDecay by remember { mutableStateOf(task?.useCustomDecay ?: false) }
    var customFormula by remember {
        mutableStateOf(task?.customDecayFormula ?: settings?.defaultFormula ?: DecayFormula.EXPONENTIAL)
    }
    var customDecayRate by remember {
        mutableDoubleStateOf(task?.customDecayRate ?: settings?.defaultDecayRate ?: 0.025)
    }
    var customMinFloor by remember {
        mutableDoubleStateOf(task?.customMinEfficiency ?: settings?.defaultMinEfficiency ?: 0.10)
    }

    var titleError by remember { mutableStateOf(false) }

    // Live preview config for the embedded chart
    val previewBaseGain = baseGainText.toDoubleOrNull() ?: 0.01
    val previewConfig = remember(useCustomDecay, customFormula, customDecayRate, customMinFloor, settings) {
        if (useCustomDecay) {
            DecayConfig(
                isEnabled = settings?.decayEnabled ?: true,
                formula = customFormula,
                decayRate = customDecayRate,
                minEfficiency = customMinFloor
            )
        } else {
            DecayConfig(
                isEnabled = settings?.decayEnabled ?: true,
                formula = settings?.defaultFormula ?: DecayFormula.EXPONENTIAL,
                decayRate = settings?.defaultDecayRate ?: 0.025,
                minEfficiency = settings?.defaultMinEfficiency ?: 0.10
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (task == null) "افزودن تسک جدید" else "ویرایش تسک",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Skill Selector
                Text(
                    text = "مهارت مربوطه:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (skills.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "هنوز مهارتی تعریف نشده است. لطفاً ابتدا از تب مهارت‌ها حداقل یک مهارت بسازید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        skills.forEach { skill ->
                            val isSelected = skill.id == selectedSkillId
                            val skillColor = parseColorSafe(skill.colorHex)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSkillId = skill.id },
                                label = { Text(skill.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = skillColor.copy(alpha = 0.2f),
                                    selectedLabelColor = skillColor
                                ),
                                modifier = Modifier.testTag("chip_skill_${skill.id}")
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("عنوان تسک (مثلاً: حل مسئله یا ۲۰ دقیقه مطالعه)") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("لطفاً عنوان تسک را وارد کنید") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_task_title")
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات اختیاری") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_task_description")
                )

                // Base Gain Percent (e.g. 0.01% to 1.0%)
                Column {
                    Text(
                        text = "میزان افزایش تسلط پایه با هر بار انجام:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "مثال: ۰.۰۱٪ برای کارهای روزمره خرد، یا ۰.۱٪ برای تسک‌های سنگین",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PRESET_GAINS.forEach { preset ->
                            val isSelected = baseGainText.toDoubleOrNull() == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) IndigoPrimary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        baseGainText = if (preset < 0.01) String.format(java.util.Locale.US, "%.4f", preset)
                                        else String.format(java.util.Locale.US, "%.2f", preset)
                                    }
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "+$preset%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = baseGainText,
                        onValueChange = { baseGainText = it },
                        label = { Text("درصد افزایش پایه (دقیق)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_task_base_gain")
                    )
                }

                // Recurrence Options
                Column {
                    Text(
                        text = "تکرار تسک:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RecurrenceType.values().forEach { rec ->
                            FilterChip(
                                selected = recurrenceType == rec,
                                onClick = { recurrenceType = rec },
                                label = { Text(rec.displayNameFa, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IndigoPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("chip_recurrence_${rec.name}")
                            )
                        }
                    }
                }

                // Diminishing Returns Section & Live Curve
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "شخصی‌سازی شیب کاهش بازدهی",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (useCustomDecay) "تنظیم پارامترهای اختصاصی برای این تسک"
                                    else "استفاده از تنظیمات پیش‌فرض برنامه",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = useCustomDecay,
                                onCheckedChange = { useCustomDecay = it },
                                modifier = Modifier.testTag("switch_custom_decay")
                            )
                        }

                        AnimatedVisibility(visible = useCustomDecay) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                // Formula
                                Text(
                                    text = "فرمول کاهش اثر:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    DecayFormula.values().forEach { formula ->
                                        FilterChip(
                                            selected = customFormula == formula,
                                            onClick = { customFormula = formula },
                                            label = { Text(formula.name.take(3), fontSize = 11.sp) },
                                            modifier = Modifier.testTag("chip_formula_${formula.name}")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Decay Rate Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "شدت شیب کاهش (Decay Rate):",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.3f", customDecayRate),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoPrimary
                                    )
                                }

                                Slider(
                                    value = customDecayRate.toFloat(),
                                    onValueChange = { customDecayRate = it.toDouble() },
                                    valueRange = 0.005f..0.080f,
                                    modifier = Modifier.fillMaxWidth().testTag("slider_task_decay_rate")
                                )

                                // Minimum Efficiency Floor Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "کف حداقل بازدهی حفظ‌شده:",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "${(customMinFloor * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAccent
                                    )
                                }

                                Slider(
                                    value = customMinFloor.toFloat(),
                                    onValueChange = { customMinFloor = it.toDouble() },
                                    valueRange = 0.05f..0.40f,
                                    modifier = Modifier.fillMaxWidth().testTag("slider_task_min_floor")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Live Embedded Curve Preview
                        Text(
                            text = "پیش‌نمایش زنده شیب تسک:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        DecayCurveChart(
                            baseGain = previewBaseGain,
                            config = previewConfig,
                            currentRepetition = task?.completionCount ?: 0,
                            maxReps = 60,
                            showControls = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    if (skills.isEmpty()) {
                        return@Button
                    }
                    val base = baseGainText.toDoubleOrNull() ?: 0.01
                    val chosenSkillId = if (selectedSkillId != 0L) selectedSkillId else (skills.firstOrNull()?.id ?: 0L)
                    onConfirm(
                        chosenSkillId,
                        title.trim(),
                        description.trim(),
                        base.coerceIn(0.0001, 10.0),
                        recurrenceType,
                        useCustomDecay,
                        customFormula,
                        customDecayRate,
                        customMinFloor
                    )
                },
                enabled = skills.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.testTag("btn_save_task")
            ) {
                Text("ذخیره تسک")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
