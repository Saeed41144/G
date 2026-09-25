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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.RecurrenceType
import com.example.data.model.SkillEntity
import com.example.data.model.TaskEntity
import com.example.ui.components.TaskCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.IndigoPrimary

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    skills: List<SkillEntity>,
    settings: AppSettingsEntity?,
    onCompleteTask: (TaskEntity) -> Unit,
    onInspectCurve: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddNewTask: () -> Unit,
    onUndoLast: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRecurrence by remember { mutableStateOf<RecurrenceType?>(null) }
    var selectedSkillFilter by remember { mutableStateOf<Long?>(null) }

    val filteredTasks = remember(tasks, selectedRecurrence, selectedSkillFilter) {
        tasks.filter { task ->
            val matchRecurrence = selectedRecurrence == null || task.recurrenceType == selectedRecurrence
            val matchSkill = selectedSkillFilter == null || task.skillId == selectedSkillFilter
            matchRecurrence && matchSkill
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewTask,
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "تسک جدید")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Info Bar with Undo Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تسک‌ها و عادات تکرارشونده",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اثر هر تسک بر اساس دفعات انجام کاهش می‌یابد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onUndoLast,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_undo_last_action")
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "بازگردانی", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بازگردانی", fontSize = 12.sp)
                    }
                }
            }

            // Recurrence Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedRecurrence == null,
                            onClick = { selectedRecurrence = null },
                            label = { Text("همه تسک‌ها", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_filter_all_recurrence")
                        )
                    }
                    items(RecurrenceType.values()) { rec ->
                        val isSelected = selectedRecurrence == rec
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRecurrence = if (isSelected) null else rec },
                            label = { Text(rec.displayNameFa, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_filter_${rec.name}")
                        )
                    }
                }
            }

            // Skill Filter Chips if multiple skills exist
            if (skills.size > 1) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSkillFilter == null,
                                onClick = { selectedSkillFilter = null },
                                label = { Text("همه مهارت‌ها", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldTertiary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(skills) { skill ->
                            val isSelected = selectedSkillFilter == skill.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSkillFilter = if (isSelected) null else skill.id },
                                label = { Text(skill.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldTertiary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Tasks List
            if (filteredTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tasks.isEmpty()) "هیچ تسکی تعریف نشده است. با دکمه + اولین تسک خود را اضافه کنید."
                            else "تسکی با این فیلترها یافت نشد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    val skill = skills.find { it.id == task.skillId }
                    TaskCard(
                        task = task,
                        skill = skill,
                        settings = settings,
                        onComplete = { onCompleteTask(task) },
                        onInspectCurve = { onInspectCurve(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }
}
