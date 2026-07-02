package com.aegis.agents

import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class GuardianCore(
    private val agents: List<GuardianAgent>,
    private val memoryRepository: GuardianMemoryRepository? = null
) {
    private val engine = GuardianEngine(agents, memoryRepository)
    private val _analysisResults = MutableSharedFlow<AnalysisResult>()
    val analysisResults = _analysisResults.asSharedFlow()

    suspend fun analyze(context: AnalysisContext): AnalysisResult {
        val result = engine.analyze(context)
        _analysisResults.emit(result)
        return result
    }

    suspend fun analyzeAll(text: String, sourceApp: String? = null): List<AgentResult> {
        val context = AnalysisContext(text = text, sourceApp = sourceApp)
        val result = engine.analyze(context)
        return result.agentResults
    }

    fun getOverallSafetyScore(): Float {
        val history = engine.analysisHistory
        if (history.isEmpty()) return 1.0f

        val recentHistory = history.takeLast(100)
        val totalScore = recentHistory.sumOf { result ->
            val threatPenalty = result.overallThreatLevel.value * 0.25
            (1.0 - threatPenalty).coerceIn(0.0, 1.0)
        }
        return (totalScore / recentHistory.size).toFloat().coerceIn(0f, 1f)
    }

    fun getAgentStatuses(): List<AgentStatus> {
        return agents.map { agent ->
            AgentStatus(
                name = agent.name,
                version = agent.version,
                isAvailable = agent.isAvailable(),
                description = agent.description
            )
        }
    }

    fun getRecentThreats(limit: Int = 10): List<AnalysisResult> {
        return engine.analysisHistory
            .filter { it.overallThreatLevel.value >= ThreatLevel.SUSPICIOUS.value }
            .takeLast(limit)
            .reversed()
    }

    fun getAnalysisHistory(): List<AnalysisResult> = engine.analysisHistory.toList()

    val agentCount: Int get() = agents.size

    val availableAgentCount: Int get() = agents.count { it.isAvailable() }

    val engineInstance: GuardianEngine get() = engine
}

data class AgentStatus(
    val name: String,
    val version: String,
    val isAvailable: Boolean,
    val description: String
)
