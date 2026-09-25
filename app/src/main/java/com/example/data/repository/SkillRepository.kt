package com.example.data.repository

import com.example.data.dao.AppSettingsDao
import com.example.data.dao.SkillDao
import com.example.data.dao.TaskDao
import com.example.data.dao.TaskLogDao
import com.example.data.model.AppSettingsEntity
import com.example.data.model.SkillEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskLogEntity
import com.example.domain.DecayCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class SkillRepository(
    private val skillDao: SkillDao,
    private val taskDao: TaskDao,
    private val taskLogDao: TaskLogDao,
    private val appSettingsDao: AppSettingsDao
) {
    val allSkills: Flow<List<SkillEntity>> = skillDao.getAllSkills()
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val recentLogs: Flow<List<TaskLogEntity>> = taskLogDao.getRecentLogs()
    val appSettings: Flow<AppSettingsEntity?> = appSettingsDao.getSettings()

    fun getTasksForSkill(skillId: Long): Flow<List<TaskEntity>> = taskDao.getTasksBySkillId(skillId)
    fun getLogsForSkill(skillId: Long): Flow<List<TaskLogEntity>> = taskLogDao.getLogsForSkill(skillId)
    fun getLogsForTask(taskId: Long): Flow<List<TaskLogEntity>> = taskLogDao.getLogsForTask(taskId)

    suspend fun insertSkill(skill: SkillEntity): Long = skillDao.insertSkill(skill)
    suspend fun updateSkill(skill: SkillEntity) = skillDao.updateSkill(skill)
    suspend fun deleteSkill(skill: SkillEntity) = skillDao.deleteSkill(skill)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun updateSettings(settings: AppSettingsEntity) = appSettingsDao.insertOrUpdate(settings)

    suspend fun clearAllData() {
        taskLogDao.deleteAllLogs()
        taskDao.deleteAllTasks()
        skillDao.deleteAllSkills()
    }

    /**
     * Executes task completion with diminishing returns calculation:
     * 1. Resolves decay config (task custom or global settings)
     * 2. Computes marginal gain
     * 3. Increments completion count and updates streak
     * 4. Updates skill's mastery (up to 100.0%)
     * 5. Creates log record
     */
    suspend fun completeTask(task: TaskEntity): TaskLogEntity {
        val settings = appSettingsDao.getSettingsDirect()
        val repetitionIndex = task.completionCount // 0-based index for decay formula

        val config = DecayCalculator.resolveConfig(task, settings)
        val efficiency = DecayCalculator.calculateEfficiency(repetitionIndex, config)
        val effectiveGain = task.baseGainPercent * efficiency

        val now = System.currentTimeMillis()
        val newCount = task.completionCount + 1

        // Simple streak logic (e.g. if completed within 36 hours for daily task)
        val isConsecutive = task.lastCompletedAt != null &&
                (now - task.lastCompletedAt) < (36 * 3600 * 1000L)
        val newStreak = if (isConsecutive) task.streakCount + 1 else 1

        // Update task
        taskDao.updateCompletion(
            taskId = task.id,
            count = newCount,
            timestamp = now,
            streak = newStreak
        )

        // Update skill mastery
        val skill = skillDao.getSkillByIdDirect(task.skillId)
        if (skill != null) {
            val newMastery = (skill.currentMastery + effectiveGain).coerceAtMost(100.0)
            skillDao.updateMastery(skill.id, newMastery)
        }

        // Insert log
        val log = TaskLogEntity(
            taskId = task.id,
            skillId = task.skillId,
            repetitionIndex = newCount,
            baseGain = task.baseGainPercent,
            effectiveGain = effectiveGain,
            efficiencyRatio = efficiency,
            timestamp = now
        )
        val logId = taskLogDao.insertLog(log)
        return log.copy(id = logId)
    }

    /**
     * Undoes the last task completion.
     */
    suspend fun undoLatestLog(): Boolean {
        val latestLog = taskLogDao.getLatestLog() ?: return false
        val task = taskDao.getTaskByIdDirect(latestLog.taskId)
        val skill = skillDao.getSkillByIdDirect(latestLog.skillId)

        if (task != null) {
            val newCount = (task.completionCount - 1).coerceAtLeast(0)
            val newStreak = (task.streakCount - 1).coerceAtLeast(0)
            taskDao.updateCompletion(
                taskId = task.id,
                count = newCount,
                timestamp = System.currentTimeMillis(),
                streak = newStreak
            )
        }

        if (skill != null) {
            val newMastery = (skill.currentMastery - latestLog.effectiveGain).coerceAtLeast(0.0)
            skillDao.updateMastery(skill.id, newMastery)
        }

        taskLogDao.deleteLog(latestLog)
        return true
    }
}
