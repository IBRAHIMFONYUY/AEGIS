package com.aegis.agents

import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
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

    fun getDetailedGuardianScore(): GuardianScore {
        val history = engine.analysisHistory
        if (history.isEmpty()) return GuardianScore(1f, 1f, 1f, 1f, 1f)

        val recentHistory = history.takeLast(100)
        
        val privacyPenalty = calculateCategoryPenalty(recentHistory, "PrivacyAgent")
        val scamPenalty = calculateCategoryPenalty(recentHistory, "ScamAgent")
        val behaviorPenalty = calculateCategoryPenalty(recentHistory, "IntentAgent")
        val contentPenalty = calculateCategoryPenalty(recentHistory, listOf("CyberbullyingAgent", "MisinformationAgent"))

        val privacy = (1.0f - privacyPenalty).coerceIn(0f, 1f)
        val scam = (1.0f - scamPenalty).coerceIn(0f, 1f)
        val behavior = (1.0f - behaviorPenalty).coerceIn(0f, 1f)
        val content = (1.0f - contentPenalty).coerceIn(0f, 1f)

        val overall = (privacy * 0.3f + scam * 0.4f + behavior * 0.2f + content * 0.1f)

        return GuardianScore(
            overall = overall,
            privacy = privacy,
            scamProtection = scam,
            deviceSecurity = behavior, // Mapping behavior/intent to device security for now
            digitalWellbeing = content
        )
    }

    private fun calculateCategoryPenalty(history: List<AnalysisResult>, agentName: String): Float {
        return calculateCategoryPenalty(history, listOf(agentName))
    }

    private fun calculateCategoryPenalty(history: List<AnalysisResult>, agentNames: List<String>): Float {
        val relevantResults = history.flatMap { it.agentResults }
            .filter { it.agentName in agentNames }
        
        if (relevantResults.isEmpty()) return 0f
        
        return relevantResults.sumOf { it.threatLevel.value * 0.1 }.toFloat() / relevantResults.size
    }

    fun getOverallSafetyScore(): Float = getDetailedGuardianScore().overall

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
