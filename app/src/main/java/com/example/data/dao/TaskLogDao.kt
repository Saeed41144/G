package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TaskLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskLogDao {
    @Query("SELECT * FROM task_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId ORDER BY repetitionIndex ASC")
    fun getLogsForTask(taskId: Long): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs WHERE skillId = :skillId ORDER BY timestamp DESC")
    fun getLogsForSkill(skillId: Long): Flow<List<TaskLogEntity>>

    @Query("SELECT * FROM task_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLog(): TaskLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TaskLogEntity): Long

    @Delete
    suspend fun deleteLog(log: TaskLogEntity)

    @Query("DELETE FROM task_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM task_logs")
    suspend fun deleteAllLogs()
}
