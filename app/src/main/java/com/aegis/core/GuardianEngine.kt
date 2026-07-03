package com.aegis.core

import com.aegis.data.repository.GuardianMemoryRepository

class GuardianEngine(
    private val agents: List<GuardianAgent>,
    private val memory: GuardianMemoryRepository? = null
) {
    private val _analysisHistory = mutableListOf<AnalysisResult>()
    val analysisHistory: List<AnalysisResult> get() = _analysisHistory.toList()

    suspend fun analyze(context: AnalysisContext): AnalysisResult {
        // Step 1: Run fast classifiers
        val fastAgents = agents.filter { it.name != "GuardianCoach" }
        val coachAgent = agents.find { it.name == "GuardianCoach" }

        val fastResults = fastAgents
            .filter { it.isAvailable() }
            .map { agent ->
                try {
                    agent.analyze(context, memory, emptyList())
                } catch (e: Exception) {
                    AgentResult(
                        agentName = agent.name,
                        threatLevel = ThreatLevel.SAFE,
                        confidence = 0f,
                        reason = "Error: ${e.message}"
                    )
                }
            }

        val overallThreat = computeOverallThreat(fastResults)
        val finalResults = fastResults.toMutableList()

        // Step 2: Run deep reasoning (Coach) for comprehensive AI analysis
        // We run it if overall threat is at least SUSPICIOUS, or if explicitly requested for accuracy
        if ((overallThreat.value >= ThreatLevel.SUSPICIOUS.value || context.metadata.containsKey("deep_scan")) 
            && coachAgent?.isAvailable() == true) {
            try {
                val coachResult = coachAgent.analyze(context, memory, fastResults)
                finalResults.add(coachResult)
            } catch (_: Exception) {
            }
        }

        val result = AnalysisResult(
            context = context,
            agentResults = finalResults,
            overallThreatLevel = overallThreat,
            timestamp = System.currentTimeMillis()
        )
        _analysisHistory.add(result)
        return result
    }

    private fun computeOverallThreat(results: List<AgentResult>): ThreatLevel {
        if (results.isEmpty()) return ThreatLevel.SAFE
        val maxThreat = results.maxBy { it.threatLevel.value }.threatLevel
        val highConfidenceThreats = results.filter {
            it.confidence >= 0.7f && it.threatLevel.value >= ThreatLevel.MALICIOUS.value
        }
        return when {
            highConfidenceThreats.size >= 2 -> ThreatLevel.CRITICAL
            maxThreat == ThreatLevel.CRITICAL -> ThreatLevel.CRITICAL
            maxThreat == ThreatLevel.MALICIOUS -> ThreatLevel.MALICIOUS
            maxThreat == ThreatLevel.LIKELY_MALICIOUS -> ThreatLevel.LIKELY_MALICIOUS
            maxThreat == ThreatLevel.SUSPICIOUS -> ThreatLevel.SUSPICIOUS
            else -> ThreatLevel.SAFE
        }
    }

    fun clearHistory() {
        _analysisHistory.clear()
    }
}

data class AnalysisResult(
    val context: AnalysisContext,
    val agentResults: List<AgentResult>,
    val overallThreatLevel: ThreatLevel,
    val timestamp: Long
)
