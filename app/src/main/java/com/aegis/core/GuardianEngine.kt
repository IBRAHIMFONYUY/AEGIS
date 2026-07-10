package com.aegis.core

import com.aegis.ai.GemmaInferenceEngine
import com.aegis.agents.ScamAgent
import com.aegis.data.repository.GuardianMemoryRepository

class GuardianEngine(
    private val agents: List<GuardianAgent>,
    private val memory: GuardianMemoryRepository? = null,
    private val gemmaEngine: GemmaInferenceEngine? = null
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

        // Step 2: Run deep reasoning (Coach) ONLY if explicitly triggered or for extremely dangerous threats
        // --- QUOTA FIX: NEVER run AI automatically in background ---
        val isExplicitTrigger = context.metadata.containsKey("deep_scan") || 
                               context.metadata.containsKey("force_ai_analysis") ||
                               context.metadata["source_type"] == "CHATBOT"
        
        // Only run coach in background if it's CRITICAL (life safety) and even then, only if allowed
        val isExtremelyCritical = overallThreat == ThreatLevel.CRITICAL && 
                                 context.metadata["source_type"] != "NOTIFICATION"

        if ((isExplicitTrigger || isExtremelyCritical) && coachAgent?.isAvailable() == true) {
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
        
        // Filter for "Real & Legit" threats: Must have high confidence (>0.6) 
        // to be considered for more than just SUSPICIOUS.
        val validResults = results.filter { it.confidence >= 0.6f }
        
        if (validResults.isEmpty()) {
            // Check if there are any results at all, even low confidence
            val maxRawThreat = results.maxByOrNull { it.threatLevel.value }?.threatLevel ?: ThreatLevel.SAFE
            return if (maxRawThreat.value >= ThreatLevel.MALICIOUS.value) {
                // Downgrade low-confidence malicious hits to SUSPICIOUS (Context not strong enough)
                ThreatLevel.SUSPICIOUS
            } else {
                ThreatLevel.SAFE
            }
        }

        val maxThreat = validResults.maxByOrNull { it.threatLevel.value }?.threatLevel ?: ThreatLevel.SAFE
        val criticalConfidenceThreats = validResults.filter {
            it.confidence >= 0.85f && it.threatLevel.value >= ThreatLevel.MALICIOUS.value
        }

        return when {
            criticalConfidenceThreats.isNotEmpty() -> ThreatLevel.CRITICAL
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
