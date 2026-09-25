package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.DecayFormula
import com.example.data.model.RecurrenceType
import com.example.data.model.SkillEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskLogEntity
import com.example.data.repository.SkillRepository
import com.example.domain.DecayCalculator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val titleFa: String, val titleEn: String) {
    SKILLS("مهارت‌ها", "Skills"),
    TASKS("تسک‌ها", "Tasks"),
    ANALYSIS("نمودار کاهش", "Decay Curve"),
    SETTINGS("تنظیمات", "Settings")
}

sealed class UiEvent {
    data class ShowMessage(val message: String, val canUndo: Boolean = false) : UiEvent()
}

class SkillViewModel(
    application: Application,
    private val repository: SkillRepository
) : AndroidViewModel(application) {

    val skills: StateFlow<List<SkillEntity>> = repository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<TaskLogEntity>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettingsEntity> = repository.appSettings
        .map { it ?: AppSettingsEntity() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppSettingsEntity()
        )

    init {
        // Automatically clear any leftover test/seed skills, tasks, and logs from previous sessions
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    private val _currentTab = MutableStateFlow(AppTab.SKILLS)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedSkillForDetail = MutableStateFlow<SkillEntity?>(null)
    val selectedSkillForDetail: StateFlow<SkillEntity?> = _selectedSkillForDetail.asStateFlow()

    private val _selectedTaskIdForAnalysis = MutableStateFlow<Long?>(null)
    val selectedTaskIdForAnalysis: StateFlow<Long?> = _selectedTaskIdForAnalysis.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectSkillForDetail(skill: SkillEntity?) {
        _selectedSkillForDetail.value = skill
    }

    fun selectTaskForAnalysis(taskId: Long?) {
        _selectedTaskIdForAnalysis.value = taskId
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                val log = repository.completeTask(task)
                triggerHapticFeedback()

                val formattedGain = DecayCalculator.formatGainPercent(log.effectiveGain)
                val efficiencyPercent = (log.efficiencyRatio * 100).toInt()
                val skill = skills.value.find { it.id == task.skillId }
                val skillName = skill?.name ?: "مهارت"

                val message = "انجام شد: $formattedGain به $skillName افزوده شد (بازدهی اثر: $efficiencyPercent٪)"
                _uiEvents.emit(UiEvent.ShowMessage(message, canUndo = true))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowMessage("خطا در ثبت تسک: ${e.localizedMessage}"))
            }
        }
    }

    fun undoLastCompletion() {
        viewModelScope.launch {
            val undone = repository.undoLatestLog()
            if (undone) {
                _uiEvents.emit(UiEvent.ShowMessage("آخرین انجام لغو شد و تسلط بازیابی شد"))
            }
        }
    }

    fun saveSkill(
        id: Long = 0,
        name: String,
        category: String,
        colorHex: String,
        iconName: String,
        targetMastery: Double
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                repository.insertSkill(
                    SkillEntity(
                        name = name,
                        category = category,
                        colorHex = colorHex,
                        iconName = iconName,
                        targetMastery = targetMastery
                    )
                )
                _uiEvents.emit(UiEvent.ShowMessage("مهارت جدید «$name» با موفقیت ساخته شد"))
            } else {
                val existing = skills.value.find { it.id == id }
                if (existing != null) {
                    repository.updateSkill(
                        existing.copy(
                            name = name,
                            category = category,
                            colorHex = colorHex,
                            iconName = iconName,
                            targetMastery = targetMastery
                        )
                    )
                    _uiEvents.emit(UiEvent.ShowMessage("مهارت ویرایش شد"))
                }
            }
        }
    }

    fun deleteSkill(skill: SkillEntity) {
        viewModelScope.launch {
            repository.deleteSkill(skill)
            if (_selectedSkillForDetail.value?.id == skill.id) {
                _selectedSkillForDetail.value = null
            }
            _uiEvents.emit(UiEvent.ShowMessage("مهارت «${skill.name}» حذف گردید"))
        }
    }

    fun saveTask(
        id: Long = 0,
        skillId: Long,
        title: String,
        description: String,
        baseGainPercent: Double,
        recurrenceType: RecurrenceType,
        useCustomDecay: Boolean,
        customDecayFormula: DecayFormula,
        customDecayRate: Double,
        customMinEfficiency: Double
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                repository.insertTask(
                    TaskEntity(
                        skillId = skillId,
                        title = title,
                        description = description,
                        baseGainPercent = baseGainPercent,
                        recurrenceType = recurrenceType,
                        useCustomDecay = useCustomDecay,
                        customDecayFormula = customDecayFormula,
                        customDecayRate = customDecayRate,
                        customMinEfficiency = customMinEfficiency
                    )
                )
                _uiEvents.emit(UiEvent.ShowMessage("تسک «$title» اضافه شد"))
            } else {
                val existing = tasks.value.find { it.id == id }
                if (existing != null) {
                    repository.updateTask(
                        existing.copy(
                            skillId = skillId,
                            title = title,
                            description = description,
                            baseGainPercent = baseGainPercent,
                            recurrenceType = recurrenceType,
                            useCustomDecay = useCustomDecay,
                            customDecayFormula = customDecayFormula,
                            customDecayRate = customDecayRate,
                            customMinEfficiency = customMinEfficiency
                        )
                    )
                    _uiEvents.emit(UiEvent.ShowMessage("تسک به روز شد"))
                }
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _uiEvents.emit(UiEvent.ShowMessage("تسک «${task.title}» حذف شد"))
        }
    }

    fun updateSettings(
        decayEnabled: Boolean,
        defaultFormula: DecayFormula,
        defaultDecayRate: Double,
        defaultMinEfficiency: Double,
        language: String,
        hapticFeedback: Boolean
    ) {
        viewModelScope.launch {
            val updated = settings.value.copy(
                decayEnabled = decayEnabled,
                defaultFormula = defaultFormula,
                defaultDecayRate = defaultDecayRate,
                defaultMinEfficiency = defaultMinEfficiency,
                language = language,
                hapticFeedback = hapticFeedback
            )
            repository.updateSettings(updated)
            _uiEvents.emit(UiEvent.ShowMessage("تنظیمات منحنی کاهش بازدهی ذخیره شد"))
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication(), viewModelScope)
            AppDatabase.populateInitialData(db)
            _uiEvents.emit(UiEvent.ShowMessage("داده‌های نمونه بارگذاری شدند"))
        }
    }

    fun clearAllUserData() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedSkillForDetail.value = null
            _selectedTaskIdForAnalysis.value = null
            _uiEvents.emit(UiEvent.ShowMessage("تمام مهارت‌ها و تسک‌های تستی با موفقیت حذف شدند"))
        }
    }

    private fun triggerHapticFeedback() {
        if (!settings.value.hapticFeedback) return
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(30)
                }
            }
        } catch (_: Exception) {}
    }
}

class SkillViewModelFactory(
    private val application: Application,
    private val repository: SkillRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SkillViewModel::class.java)) {
            return SkillViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
