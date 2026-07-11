package com.aegis.ai.analytics

import com.aegis.core.AnalysisResult
import com.aegis.core.ThreatLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityAnalyticsManager @Inject constructor() {

    private val _analytics = MutableStateFlow(SecurityStats())
    val analytics: StateFlow<SecurityStats> = _analytics.asStateFlow()

    fun recordAnalysis(result: AnalysisResult) {
        val current = _analytics.value
        val isThreat = result.overallThreatLevel.value >= ThreatLevel.MALICIOUS.value
        
        val newAppStats = current.appMonitoringStats.toMutableMap()
        val appName = result.context.sourceApp?.split('.')?.lastOrNull() ?: "System"
        val appStats = newAppStats[appName] ?: AppStats()
        
        newAppStats[appName] = appStats.copy(
            totalScans = appStats.totalScans + 1,
            threatsFound = if (isThreat) appStats.threatsFound + 1 else appStats.threatsFound
        )

        _analytics.value = current.copy(
            totalMessagesAnalyzed = current.totalMessagesAnalyzed + 1,
            totalThreatsBlocked = if (isThreat) current.totalThreatsBlocked + 1 else current.totalThreatsBlocked,
            averageConfidence = (current.averageConfidence * current.totalMessagesAnalyzed + 
                                result.agentResults.maxOfOrNull { it.confidence }?.coerceIn(0f, 1f).let { it ?: 0.5f }) / 
                                (current.totalMessagesAnalyzed + 1),
            appMonitoringStats = newAppStats,
            lastAnalysisTime = result.timestamp
        )
    }

    data class SecurityStats(
        val totalMessagesAnalyzed: Long = 0,
        val totalThreatsBlocked: Int = 0,
        val averageConfidence: Float = 0f,
        val lastAnalysisTime: Long = 0,
        val appMonitoringStats: Map<String, AppStats> = emptyMap()
    )

    data class AppStats(
        val totalScans: Int = 0,
        val threatsFound: Int = 0
    )
}
