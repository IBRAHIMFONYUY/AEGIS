package com.aegis.data.repository

import com.aegis.data.db.dao.SafetyScoreDao
import com.aegis.data.db.entity.SafetyScore
import kotlinx.coroutines.flow.Flow

class SafetyRepository(private val safetyScoreDao: SafetyScoreDao) {

    fun getLatestScore(): Flow<SafetyScore?> = safetyScoreDao.getLatestScore()

    fun getScoreHistory(): Flow<List<SafetyScore>> = safetyScoreDao.getScoreHistory()

    suspend fun getAverageScoreSince(since: Long): Float =
        safetyScoreDao.getAverageScoreSince(since)

    suspend fun recordScore(score: SafetyScore) {
        safetyScoreDao.insert(score)
    }

    suspend fun deleteOldScores(before: Long) {
        safetyScoreDao.deleteOldScores(before)
    }
}
