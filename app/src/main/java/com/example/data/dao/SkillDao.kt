package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SkillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY currentMastery DESC, createdAt DESC")
    fun getAllSkills(): Flow<List<SkillEntity>>

    @Query("SELECT * FROM skills WHERE id = :id")
    fun getSkillById(id: Long): Flow<SkillEntity?>

    @Query("SELECT * FROM skills WHERE id = :id")
    suspend fun getSkillByIdDirect(id: Long): SkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity): Long

    @Update
    suspend fun updateSkill(skill: SkillEntity)

    @Delete
    suspend fun deleteSkill(skill: SkillEntity)

    @Query("UPDATE skills SET currentMastery = :newMastery WHERE id = :skillId")
    suspend fun updateMastery(skillId: Long, newMastery: Double)

    @Query("DELETE FROM skills")
    suspend fun deleteAllSkills()
}
