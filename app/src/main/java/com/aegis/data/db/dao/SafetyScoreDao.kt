package com.aegis.data.db.dao

import androidx.room.*
import com.aegis.data.db.entity.SafetyScore
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyScoreDao {
    @Query("SELECT * FROM safety_scores ORDER BY timestamp DESC LIMIT 1")
    fun getLatestScore(): Flow<SafetyScore?>

    @Query("SELECT * FROM safety_scores ORDER BY timestamp DESC LIMIT 30")
    fun getScoreHistory(): Flow<List<SafetyScore>>

    @Query("SELECT AVG(score) FROM safety_scores WHERE timestamp > :since")
    suspend fun getAverageScoreSince(since: Long): Float

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: SafetyScore)

    @Query("DELETE FROM safety_scores WHERE timestamp < :before")
    suspend fun deleteOldScores(before: Long)
}
