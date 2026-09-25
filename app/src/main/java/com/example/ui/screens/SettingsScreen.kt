package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.domain.DecayConfig
import com.example.ui.components.DecayCurveChart
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoPrimary

@Composable
fun SettingsScreen(
    settings: AppSettingsEntity,
    onSaveSettings: (
        decayEnabled: Boolean,
        defaultFormula: DecayFormula,
        defaultDecayRate: Double,
        defaultMinEfficiency: Double,
        language: String,
        hapticFeedback: Boolean
    ) -> Unit,
    onClearAllData: () -> Unit,
    onResetDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var decayEnabled by remember(settings) { mutableStateOf(settings.decayEnabled) }
    var defaultFormula by remember(settings) { mutableStateOf(settings.defaultFormula) }
    var defaultDecayRate by remember(settings) { mutableDoubleStateOf(settings.defaultDecayRate) }
    var defaultMinEfficiency by remember(settings) { mutableDoubleStateOf(settings.defaultMinEfficiency) }
    var language by remember(settings) { mutableStateOf(settings.language) }
    var hapticFeedback by remember(settings) { mutableStateOf(settings.hapticFeedback) }

    val liveConfig = remember(decayEnabled, defaultFormula, defaultDecayRate, defaultMinEfficiency) {
        DecayConfig(
            isEnabled = decayEnabled,
            formula = defaultFormula,
            decayRate = defaultDecayRate,
            minEfficiency = defaultMinEfficiency
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "تنظیمات و شخصی‌سازی کاهش بازدهی",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "پیکربندی پیش‌فرض شیب کاهش اثر تسک‌ها و رفتار برنامه",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Master Decay Switch Card
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "محاسبه خودکار کاهش اثر تسک‌ها (Diminishing Returns)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "با فعال بودن این قابلیت، با افزایش تکرار هر تسک، میزان افزایش تسلط آن به مرور کاهش می‌یابد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Switch(
                            checked = decayEnabled,
                            onCheckedChange = { decayEnabled = it },
                            modifier = Modifier.testTag("switch_global_decay_enabled")
                        )
                    }

                    AnimatedVisibility(visible = decayEnabled) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Formula Choice
                            Text(
                                text = "مدل ریاضی پیش‌فرض کاهش اثر:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            DecayFormula.values().forEach { f ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = defaultFormula == f,
                                        onClick = { defaultFormula = f },
                                        label = { Text(f.displayNameFa, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = IndigoPrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("chip_settings_formula_${f.name}")
                                    )
                                }
                                Text(
                                    text = f.descriptionFa,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Slope / Decay Rate Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "شیب پیش‌فرض کاهش (Decay Slope):",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.3f", defaultDecayRate) +
                                            if (defaultDecayRate < 0.02) " (ملایم)" else if (defaultDecayRate > 0.05) " (تند)" else " (متعادل)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IndigoPrimary
                                )
                            }

                            Slider(
                                value = defaultDecayRate.toFloat(),
                                onValueChange = { defaultDecayRate = it.toDouble() },
                                valueRange = 0.005f..0.080f,
                                modifier = Modifier.fillMaxWidth().testTag("slider_settings_decay_rate")
                            )

                            // Minimum Floor Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "کف حداقل اثر حفظ‌شده (Floor):",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "${(defaultMinEfficiency * 100).toInt()}% از اثر پایه",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAccent
                                )
                            }

                            Slider(
                                value = defaultMinEfficiency.toFloat(),
                                onValueChange = { defaultMinEfficiency = it.toDouble() },
                                valueRange = 0.05f..0.45f,
                                modifier = Modifier.fillMaxWidth().testTag("slider_settings_min_floor")
                            )
                        }
                    }
                }
            }
        }

        // Live Chart Preview Inside Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "پیش‌نمایش زنده نمودار شیب پیش‌فرض",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DecayCurveChart(
                        baseGain = 0.01,
                        config = liveConfig,
                        currentRepetition = 0,
                        maxReps = 70,
                        showControls = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Save Button for Settings
        item {
            Button(
                onClick = {
                    onSaveSettings(
                        decayEnabled,
                        defaultFormula,
                        defaultDecayRate,
                        defaultMinEfficiency,
                        language,
                        hapticFeedback
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_save_settings")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ذخیره تنظیمات شیب کاهش", fontWeight = FontWeight.Bold)
            }
        }

        // General App Preferences Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ترجیحات عمومی برنامه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Haptic Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "بازخورد لمسی (ویبره کوتاه هنگام انجام تسک)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = hapticFeedback,
                            onCheckedChange = {
                                hapticFeedback = it
                                onSaveSettings(decayEnabled, defaultFormula, defaultDecayRate, defaultMinEfficiency, language, it)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Language Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "زبان رابط کاربری", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = language == "FA",
                                onClick = {
                                    language = "FA"
                                    onSaveSettings(decayEnabled, defaultFormula, defaultDecayRate, defaultMinEfficiency, "FA", hapticFeedback)
                                },
                                label = { Text("فارسی") }
                            )
                            FilterChip(
                                selected = language == "EN",
                                onClick = {
                                    language = "EN"
                                    onSaveSettings(decayEnabled, defaultFormula, defaultDecayRate, defaultMinEfficiency, "EN", hapticFeedback)
                                },
                                label = { Text("English") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Clear All Data
                    var showConfirmClearDialog by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { showConfirmClearDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = com.example.ui.theme.RoseAccent
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("btn_clear_all_data")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حذف تمام اطلاعات، مهارت‌ها و تسک‌ها")
                    }

                    if (showConfirmClearDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showConfirmClearDialog = false },
                            title = { Text("حذف تمام اطلاعات", fontWeight = FontWeight.Bold) },
                            text = { Text("آیا مطمئن هستید که می‌خواهید تمام مهارت‌ها، تسک‌ها و تاریخچه را پاک کنید؟ این عملیات غیرقابل بازگشت است.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showConfirmClearDialog = false
                                        onClearAllData()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.RoseAccent)
                                ) {
                                    Text("بله، همه را حذف کن")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showConfirmClearDialog = false }) {
                                    Text("انصراف")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Demo Data Reset
                    OutlinedButton(
                        onClick = onResetDemoData,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_reset_demo_data")
                    ) {
                        Icon(Icons.Default.Cached, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بارگذاری مجدد داده‌های نمونه مهارت و تسک")
                    }
                }
            }
        }
    }
}
