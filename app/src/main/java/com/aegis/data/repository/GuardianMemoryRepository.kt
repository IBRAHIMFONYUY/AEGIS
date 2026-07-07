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

    suspend fun storeConversationMessage(chatId: String, sender: String, message: String) {
        val historyKey = "CHAT_HISTORY_$chatId"
        val existingHistory = getLatest(historyKey, "System") ?: ""
        
        // Keep last 20 messages: "Sender: Message | Sender: Message..."
        val messages = existingHistory.split(" | ").filter { it.isNotBlank() }.toMutableList()
        messages.add("$sender: $message")
        
        val newHistory = messages.takeLast(20).joinToString(" | ")
        store(
            key = historyKey,
            value = newHistory,
            agentName = "System",
            category = "CONVERSATION_CONTEXT",
            ttlMillis = 7 * 24 * 60 * 60 * 1000L // 7 days
        )
    }

    suspend fun getConversationHistory(chatId: String): List<String> {
        val history = getLatest("CHAT_HISTORY_$chatId", "System") ?: return emptyList()
        return history.split(" | ").filter { it.isNotBlank() }
    }

    suspend fun storeContactSummary(contactId: String, summary: String) {
        store(
            key = "CONTACT_SUMMARY_$contactId",
            value = summary,
            agentName = "MemoryAgent",
            category = "CONTACT_CONTEXT",
            ttlMillis = 365 * 24 * 60 * 60 * 1000L // 1 year
        )
    }

    suspend fun getContactSummary(contactId: String): String? {
        return getLatest("CONTACT_SUMMARY_$contactId", "MemoryAgent")
    }

    suspend fun storeBehavioralData(chatId: String?, behaviorMetrics: Any, timestamp: Long) {
        if (chatId == null) return
        val key = "BEHAVIOR_$chatId"
        // Use a simple string representation or JSON if available
        store(
            key = "${key}_$timestamp",
            value = behaviorMetrics.toString(),
            agentName = "BehavioralAgent",
            category = "USER_BEHAVIOR",
            ttlMillis = 30 * 24 * 60 * 60 * 1000L // 30 days
        )
    }

    suspend fun getBehavioralHistory(chatId: String, limit: Int): List<com.aegis.agents.BehavioralMetrics> {
        // This is a bit tricky since we don't have a structured behavioral store here.
        // We'll return an empty list for now or try to parse if needed.
        // In a real app, we'd have a specific table for behavioral metrics.
        return emptyList()
    }

    suspend fun cleanup() {
        memoryDao.deleteExpired()
    }
}
