package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SkillEntity
import com.example.ui.theme.IndigoPrimary

private val AVAILABLE_COLORS = listOf(
    "#6366F1", // Indigo
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#EC4899", // Pink/Rose
    "#8B5CF6", // Violet
    "#06B6D4"  // Cyan
)

private val AVAILABLE_ICONS = listOf(
    "code" to Icons.Default.Code,
    "translate" to Icons.Default.Translate,
    "music_note" to Icons.Default.MusicNote,
    "fitness" to Icons.Default.FitnessCenter,
    "book" to Icons.Default.MenuBook,
    "palette" to Icons.Default.Palette,
    "brain" to Icons.Default.Psychology,
    "bolt" to Icons.Default.Bolt
)

private val CATEGORY_SUGGESTIONS = listOf(
    "کدنویسی و فنی",
    "زبان‌های خارجی",
    "موسیقی و هنر",
    "ورزش و تندرستی",
    "مطالعه و دانش",
    "عمومی"
)

@Composable
fun AddEditSkillDialog(
    skill: SkillEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, colorHex: String, iconName: String, target: Double) -> Unit
) {
    var name by remember { mutableStateOf(skill?.name ?: "") }
    var category by remember { mutableStateOf(skill?.category ?: "عمومی") }
    var selectedColor by remember { mutableStateOf(skill?.colorHex ?: AVAILABLE_COLORS.first()) }
    var selectedIcon by remember { mutableStateOf(skill?.iconName ?: AVAILABLE_ICONS.first().first) }
    var targetText by remember { mutableStateOf(skill?.targetMastery?.toInt()?.toString() ?: "100") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (skill == null) "تعریف مهارت جدید" else "ویرایش مهارت",
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
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) hasError = false
                    },
                    label = { Text("نام مهارت (مثلاً: زبان آلمانی یا پایتون)") },
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text("لطفاً نام مهارت را وارد کنید") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_skill_name")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("دسته‌بندی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_skill_category")
                )

                // Quick Category Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CATEGORY_SUGGESTIONS.take(3).forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (category == cat) IndigoPrimary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { category = cat }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (category == cat) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Color Selection
                Text(
                    text = "رنگ نشانگر مهارت:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AVAILABLE_COLORS.forEach { hex ->
                        val color = parseColorSafe(hex)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .testTag("color_picker_$hex")
                        )
                    }
                }

                // Icon Selection
                Text(
                    text = "آیکون مهارت:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AVAILABLE_ICONS.take(6).forEach { (iconKey, iconVector) ->
                        val isSelected = selectedIcon == iconKey
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) IndigoPrimary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIcon = iconKey }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, IndigoPrimary, RoundedCornerShape(10.dp))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = iconKey,
                                tint = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Target Mastery Percent
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("هدف تسلط نهایی (درصد، پیش‌فرض ۱۰۰)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_skill_target")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        hasError = true
                        return@Button
                    }
                    val targetVal = targetText.toDoubleOrNull() ?: 100.0
                    onConfirm(
                        name.trim(),
                        category.trim().ifBlank { "عمومی" },
                        selectedColor,
                        selectedIcon,
                        targetVal.coerceIn(1.0, 100.0)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                modifier = Modifier.testTag("btn_save_skill")
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
