package com.aegis.data.repository

import com.aegis.data.db.dao.MemoryDao
import com.aegis.data.db.entity.MemoryEntry
import kotlinx.coroutines.flow.Flow

class GuardianMemoryRepository(private val memoryDao: MemoryDao) {

    suspend fun store(
        key: String,
        value: String,
        agentName: String? = null,
        category: String = "GENERAL",
        ttlMillis: Long? = null
    ) {
        val expiresAt = ttlMillis?.let { System.currentTimeMillis() + it }
        val entry = MemoryEntry(
            key = key,
            value = value,
            agentName = agentName,
            category = category,
            expiresAt = expiresAt
        )
        memoryDao.insert(entry)
    }

    suspend fun getLatest(key: String, agentName: String? = null): String? {
        return memoryDao.getLatest(key, agentName)?.value
    }

    fun getByCategory(category: String): Flow<List<MemoryEntry>> {
        return memoryDao.getByCategory(category)
    }

    suspend fun getForAgent(agentName: String): Map<String, String> {
        return memoryDao.getForAgent(agentName).associate { it.key to it.value }
    }

    suspend fun recordUserAction(agentName: String, action: String, threatId: String) {
        store(
            key = "USER_ACTION_$threatId",
            value = action,
            agentName = agentName,
            category = "USER_BEHAVIOR",
            ttlMillis = 30 * 24 * 60 * 60 * 1000L // 30 days
        )
        
        // Update global ignore count if applicable
        if (action == "IGNORE") {
            val currentCount = getLatest("GLOBAL_IGNORE_COUNT")?.toIntOrNull() ?: 0
            store(
                key = "GLOBAL_IGNORE_COUNT",
                value = (currentCount + 1).toString(),
                category = "USER_BEHAVIOR"
            )
        }
    }

    suspend fun cleanup() {
        memoryDao.deleteExpired()
    }
}
