package com.aegis.data.db.dao

import androidx.room.*
import com.aegis.data.db.entity.LearningProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_progress ORDER BY lastAccessed DESC")
    fun getAllModules(): Flow<List<LearningProgress>>

    @Query("SELECT * FROM learning_progress WHERE moduleId = :moduleId")
    suspend fun getModule(moduleId: String): LearningProgress?

    @Query("SELECT COUNT(*) FROM learning_progress WHERE completed = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT SUM(score) FROM learning_progress")
    fun getTotalScore(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LearningProgress)

    @Query("UPDATE learning_progress SET completed = :completed, score = :score, completedAt = :completedAt WHERE moduleId = :moduleId")
    suspend fun completeModule(moduleId: String, completed: Boolean = true, score: Int = 100, completedAt: Long = System.currentTimeMillis())
}
