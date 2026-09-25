package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.model.SkillEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.SkillRepository
import com.example.ui.components.AddEditSkillDialog
import com.example.ui.components.AddEditTaskDialog
import com.example.ui.components.SkillDetailSheet
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.SkillsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SkillCurveTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.SkillViewModel
import com.example.ui.viewmodel.SkillViewModelFactory
import com.example.ui.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: SkillViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
        val database = AppDatabase.getDatabase(applicationContext, coroutineScope)
        val repository = SkillRepository(
            skillDao = database.skillDao(),
            taskDao = database.taskDao(),
            taskLogDao = database.taskLogDao(),
            appSettingsDao = database.appSettingsDao()
        )
        val factory = SkillViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[SkillViewModel::class.java]

        setContent {
            SkillCurveTheme {
                SkillCurveApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SkillCurveApp(viewModel: SkillViewModel) {
    val skills by viewModel.skills.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedSkillForDetail by viewModel.selectedSkillForDetail.collectAsStateWithLifecycle()
    val selectedTaskIdForAnalysis by viewModel.selectedTaskIdForAnalysis.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dialog state holders
    var showAddEditSkillDialog by remember { mutableStateOf(false) }
    var skillToEdit by remember { mutableStateOf<SkillEntity?>(null) }

    var showAddEditTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var presetSkillIdForTask by remember { mutableStateOf<Long?>(null) }

    // Listen for UI events & snackbars
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = if (event.canUndo) "لغو" else null,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed && event.canUndo) {
                            viewModel.undoLastCompletion()
                        }
                    }
                }
            }
        }
    }

    // Support RTL when language is Persian
    val layoutDirection = if (settings.language == "EN") LayoutDirection.Ltr else LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        // Back navigation handling
        BackHandler(enabled = currentTab != AppTab.SKILLS || selectedSkillForDetail != null) {
            if (selectedSkillForDetail != null) {
                viewModel.selectSkillForDetail(null)
            } else if (currentTab != AppTab.SKILLS) {
                viewModel.setTab(AppTab.SKILLS)
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    val isFa = settings.language != "EN"

                    NavigationBarItem(
                        selected = currentTab == AppTab.SKILLS,
                        onClick = { viewModel.setTab(AppTab.SKILLS) },
                        icon = { Icon(Icons.Default.Bolt, contentDescription = "مهارت‌ها") },
                        label = { Text(if (isFa) AppTab.SKILLS.titleFa else AppTab.SKILLS.titleEn, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("tab_skills")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.TASKS,
                        onClick = { viewModel.setTab(AppTab.TASKS) },
                        icon = { Icon(Icons.Default.TaskAlt, contentDescription = "تسک‌ها") },
                        label = { Text(if (isFa) AppTab.TASKS.titleFa else AppTab.TASKS.titleEn, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("tab_tasks")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.ANALYSIS,
                        onClick = { viewModel.setTab(AppTab.ANALYSIS) },
                        icon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "نمودار") },
                        label = { Text(if (isFa) AppTab.ANALYSIS.titleFa else AppTab.ANALYSIS.titleEn, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("tab_analysis")
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.SETTINGS,
                        onClick = { viewModel.setTab(AppTab.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") },
                        label = { Text(if (isFa) AppTab.SETTINGS.titleFa else AppTab.SETTINGS.titleEn, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndigoPrimary,
                            selectedTextColor = IndigoPrimary,
                            indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_animation"
                ) { tab ->
                    when (tab) {
                        AppTab.SKILLS -> SkillsScreen(
                            skills = skills,
                            tasks = tasks,
                            onSkillClick = { skill ->
                                viewModel.selectSkillForDetail(skill)
                            },
                            onAddNewSkill = {
                                skillToEdit = null
                                showAddEditSkillDialog = true
                            },
                            onAddTaskToSkill = { skill ->
                                taskToEdit = null
                                presetSkillIdForTask = skill.id
                                showAddEditTaskDialog = true
                            }
                        )

                        AppTab.TASKS -> TasksScreen(
                            tasks = tasks,
                            skills = skills,
                            settings = settings,
                            onCompleteTask = { task ->
                                viewModel.completeTask(task)
                            },
                            onInspectCurve = { task ->
                                viewModel.selectTaskForAnalysis(task.id)
                                viewModel.setTab(AppTab.ANALYSIS)
                            },
                            onEditTask = { task ->
                                taskToEdit = task
                                presetSkillIdForTask = task.skillId
                                showAddEditTaskDialog = true
                            },
                            onDeleteTask = { task ->
                                viewModel.deleteTask(task)
                            },
                            onAddNewTask = {
                                taskToEdit = null
                                presetSkillIdForTask = null
                                showAddEditTaskDialog = true
                            },
                            onUndoLast = {
                                viewModel.undoLastCompletion()
                            }
                        )

                        AppTab.ANALYSIS -> AnalysisScreen(
                            tasks = tasks,
                            skills = skills,
                            settings = settings,
                            selectedTaskId = selectedTaskIdForAnalysis,
                            onUpdateTaskCustomDecay = { task, formula, rate, floor ->
                                viewModel.saveTask(
                                    id = task.id,
                                    skillId = task.skillId,
                                    title = task.title,
                                    description = task.description,
                                    baseGainPercent = task.baseGainPercent,
                                    recurrenceType = task.recurrenceType,
                                    useCustomDecay = true,
                                    customDecayFormula = formula,
                                    customDecayRate = rate,
                                    customMinEfficiency = floor
                                )
                            }
                        )

                        AppTab.SETTINGS -> SettingsScreen(
                            settings = settings,
                            onSaveSettings = { decayEnabled, formula, rate, floor, lang, haptic ->
                                viewModel.updateSettings(
                                    decayEnabled = decayEnabled,
                                    defaultFormula = formula,
                                    defaultDecayRate = rate,
                                    defaultMinEfficiency = floor,
                                    language = lang,
                                    hapticFeedback = haptic
                                )
                            },
                            onClearAllData = {
                                viewModel.clearAllUserData()
                            },
                            onResetDemoData = {
                                viewModel.resetDemoData()
                            }
                        )
                    }
                }
            }

            // Skill Detail Sheet
            if (selectedSkillForDetail != null) {
                val currentSkill = selectedSkillForDetail!!
                val skillTasks = tasks.filter { it.skillId == currentSkill.id }
                val skillLogs = recentLogs.filter { it.skillId == currentSkill.id }

                SkillDetailSheet(
                    skill = currentSkill,
                    tasks = skillTasks,
                    logs = skillLogs,
                    onDismiss = { viewModel.selectSkillForDetail(null) },
                    onEditSkill = {
                        skillToEdit = currentSkill
                        showAddEditSkillDialog = true
                    },
                    onDeleteSkill = {
                        viewModel.deleteSkill(currentSkill)
                    },
                    onAddTask = {
                        taskToEdit = null
                        presetSkillIdForTask = currentSkill.id
                        showAddEditTaskDialog = true
                    },
                    onCompleteTask = { task ->
                        viewModel.completeTask(task)
                    }
                )
            }

            // Add/Edit Skill Dialog
            if (showAddEditSkillDialog) {
                AddEditSkillDialog(
                    skill = skillToEdit,
                    onDismiss = { showAddEditSkillDialog = false },
                    onConfirm = { name, category, colorHex, iconName, target ->
                        viewModel.saveSkill(
                            id = skillToEdit?.id ?: 0L,
                            name = name,
                            category = category,
                            colorHex = colorHex,
                            iconName = iconName,
                            targetMastery = target
                        )
                        showAddEditSkillDialog = false
                    }
                )
            }

            // Add/Edit Task Dialog
            if (showAddEditTaskDialog) {
                AddEditTaskDialog(
                    task = taskToEdit,
                    defaultSkillId = presetSkillIdForTask,
                    skills = skills,
                    settings = settings,
                    onDismiss = { showAddEditTaskDialog = false },
                    onConfirm = { skillId, title, desc, baseGain, recurrence, useCustomDecay, formula, rate, floor ->
                        viewModel.saveTask(
                            id = taskToEdit?.id ?: 0L,
                            skillId = skillId,
                            title = title,
                            description = desc,
                            baseGainPercent = baseGain,
                            recurrenceType = recurrence,
                            useCustomDecay = useCustomDecay,
                            customDecayFormula = formula,
                            customDecayRate = rate,
                            customMinEfficiency = floor
                        )
                        showAddEditTaskDialog = false
                    }
                )
            }
        }
    }
}
