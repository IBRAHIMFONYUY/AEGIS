package com.aegis.data.repository

import com.aegis.data.db.dao.LearningDao
import com.aegis.data.db.entity.LearningProgress
import kotlinx.coroutines.flow.Flow

class LearningRepository(private val learningDao: LearningDao) {

    fun getAllModules(): Flow<List<LearningProgress>> = learningDao.getAllModules()

    suspend fun getModule(moduleId: String): LearningProgress? = learningDao.getModule(moduleId)

    fun getCompletedCount(): Flow<Int> = learningDao.getCompletedCount()

    fun getTotalScore(): Flow<Int?> = learningDao.getTotalScore()

    suspend fun upsertModule(progress: LearningProgress) {
        learningDao.upsert(progress)
    }

    suspend fun completeModule(moduleId: String, score: Int = 100) {
        learningDao.completeModule(moduleId, true, score)
    }
}
