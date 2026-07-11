package com.aegis.data.repository

import com.aegis.core.AnalysisResult
import com.aegis.core.ThreatLevel
import com.aegis.data.db.dao.ThreatDao
import com.aegis.data.db.entity.ThreatEvent
import kotlinx.coroutines.flow.Flow

class ThreatRepository(private val threatDao: ThreatDao) {

    fun getAllThreats(): Flow<List<ThreatEvent>> = threatDao.getAllThreats()

    fun getRecentThreats(minLevel: Int = 2): Flow<List<ThreatEvent>> =
        threatDao.getRecentThreats(minLevel)

    fun getCriticalThreatCount(): Flow<Int> = threatDao.getCriticalThreatCount()

    fun getTotalThreatCount(): Flow<Int> = threatDao.getTotalThreatCount()

    fun getScamsBlockedCount(since: Long): Flow<Int> = threatDao.getScamsBlockedCount(since)

    fun getLinksBlockedCount(since: Long): Flow<Int> = threatDao.getLinksBlockedCount(since)

    fun getFakeNewsDetectedCount(since: Long): Flow<Int> = threatDao.getFakeNewsDetectedCount(since)

    suspend fun getThreatById(id: Long): ThreatEvent? = threatDao.getThreatById(id)

    suspend fun saveAnalysisResult(result: AnalysisResult): Long? {
        var firstId: Long? = null
        result.agentResults.forEach { agentResult ->
            val event = ThreatEvent(
                agentName = agentResult.agentName,
                threatLevel = agentResult.threatLevel.value,
                confidence = agentResult.confidence,
                reason = agentResult.reason,
                sourceText = result.context.text ?: "",
                sourceApp = result.context.sourceApp,
                sourceType = result.context.sourceType.name,
                suggestedAction = agentResult.suggestedAction,
                details = agentResult.details.entries.joinToString("\n") { "${it.key}=${it.value}" },
                timestamp = result.timestamp
            )
            if (agentResult.threatLevel.value >= ThreatLevel.SUSPICIOUS.value) {
                val id = threatDao.insert(event)
                if (firstId == null) firstId = id
            }
        }
        return firstId
    }

    suspend fun resolveThreat(id: Long, action: String?) {
        threatDao.resolveThreat(id, true, action)
    }

    suspend fun deleteOldThreats(before: Long) {
        threatDao.deleteOldThreats(before)
    }
}
