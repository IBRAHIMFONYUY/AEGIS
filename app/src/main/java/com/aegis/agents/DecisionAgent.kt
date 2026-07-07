package com.aegis.agents

import com.aegis.ai.ReasoningEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DecisionAgent(
    private val reasoningEngine: ReasoningEngine? = null
) : GuardianAgent {

    override val name = "DecisionAgent"
    override val version = "1.0.0"
    override val description = "Synthesizes all agent results to make final security decisions and recommendations"

    override suspend fun analyze(
        context: AnalysisContext,
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult =
        withContext(Dispatchers.Default) {
            if (previousResults.isEmpty()) {
                return@withContext safeResult
            }

            // Aggregate threat levels
            val threatLevels = previousResults.map { it.threatLevel }
            val maxThreat = threatLevels.maxByOrNull { it.value } ?: ThreatLevel.SAFE
            
            // Calculate weighted confidence
            val highConfidenceThreats = previousResults.filter { 
                it.confidence >= 0.7f && it.threatLevel.value >= ThreatLevel.MALICIOUS.value 
            }
            
            // Decision logic
            val decisionScore = calculateDecisionScore(previousResults)
            val finalThreatLevel = determineFinalThreatLevel(previousResults, decisionScore)
            
            // Build comprehensive reasoning
            val reasoning = buildComprehensiveReasoning(previousResults, finalThreatLevel, decisionScore)
            
            // Generate actionable recommendation
            val recommendation = generateRecommendation(previousResults, finalThreatLevel, context)
            
            // Use reasoning engine for complex scenarios
            val aiReasoning = if (reasoningEngine != null && finalThreatLevel.value >= ThreatLevel.SUSPICIOUS.value) {
                val reasoningPrompt = "Context: ${context.text}\nAgent Results: ${previousResults.joinToString { "${it.agentName}: ${it.reason}" }}\n\nAnalyze these security analysis results and provide a comprehensive decision with actionable advice."
                reasoningEngine.generateResponse(
                    prompt = reasoningPrompt
                )
            } else null

            AgentResult(
                agentName = name,
                threatLevel = finalThreatLevel,
                confidence = decisionScore,
                reason = if (aiReasoning != null) "$reasoning\n\nAI Reasoning: $aiReasoning" else reasoning,
                details = buildDecisionDetails(previousResults, decisionScore),
                suggestedAction = recommendation,
                requiresUserAttention = finalThreatLevel.value >= ThreatLevel.MALICIOUS.value
            )
        }

    private fun calculateDecisionScore(results: List<AgentResult>): Float {
        if (results.isEmpty()) return 0f

        // Weight agents by importance
        val agentWeights = mapOf(
            "ScamAgent" to 0.25f,
            "IntentAgent" to 0.20f,
            "PhishingAgent" to 0.20f,
            "MalwareGuardianAgent" to 0.15f,
            "PrivacyAgent" to 0.10f,
            "DeepfakeAgent" to 0.05f,
            "BehavioralAgent" to 0.05f
        )

        var weightedScore = 0f
        var totalWeight = 0f

        for (result in results) {
            val weight = agentWeights[result.agentName] ?: 0.05f
            weightedScore += result.confidence * weight * result.threatLevel.value
            totalWeight += weight
        }

        return if (totalWeight > 0) weightedScore / totalWeight else 0f
    }

    private fun determineFinalThreatLevel(results: List<AgentResult>, decisionScore: Float): ThreatLevel {
        // Critical if multiple high-confidence threats
        val criticalThreats = results.count { 
            it.threatLevel == ThreatLevel.CRITICAL && it.confidence >= 0.7f 
        }
        if (criticalThreats >= 2) return ThreatLevel.CRITICAL

        // Malicious if any critical or multiple malicious
        val maliciousThreats = results.count { 
            it.threatLevel.value >= ThreatLevel.MALICIOUS.value && it.confidence >= 0.6f 
        }
        if (criticalThreats >= 1 || maliciousThreats >= 2) return ThreatLevel.MALICIOUS

        // Likely malicious if single malicious or multiple suspicious
        val suspiciousThreats = results.count { 
            it.threatLevel.value >= ThreatLevel.SUSPICIOUS.value 
        }
        if (maliciousThreats >= 1 || suspiciousThreats >= 3) return ThreatLevel.LIKELY_MALICIOUS

        // Suspicious if multiple low-level threats
        if (suspiciousThreats >= 2) return ThreatLevel.SUSPICIOUS

        // Use decision score as fallback
        return when {
            decisionScore >= 0.7f -> ThreatLevel.CRITICAL
            decisionScore >= 0.5f -> ThreatLevel.MALICIOUS
            decisionScore >= 0.3f -> ThreatLevel.LIKELY_MALICIOUS
            decisionScore >= 0.15f -> ThreatLevel.SUSPICIOUS
            else -> ThreatLevel.SAFE
        }
    }

    private fun buildComprehensiveReasoning(
        results: List<AgentResult>,
        finalThreat: ThreatLevel,
        decisionScore: Float
    ): String {
        val threatSummary = results.groupBy { it.threatLevel }
            .map { (level, agents) -> 
                "${level.name}: ${agents.size} agent(s)" 
            }
            .joinToString(", ")

        val highRiskAgents = results
            .filter { it.threatLevel.value >= ThreatLevel.MALICIOUS.value }
            .map { it.agentName }
            .joinToString(", ")

        return """
            |Decision Analysis:
            |• Overall Threat: ${finalThreat.name} (${(decisionScore * 100).toInt()}% confidence)
            |• Threat Distribution: $threatSummary
            |• High-Risk Agents: ${if (highRiskAgents.isEmpty()) "None" else highRiskAgents}
            |• Total Agents Analyzed: ${results.size}
        """.trimMargin()
    }

    private fun generateRecommendation(
        results: List<AgentResult>,
        threatLevel: ThreatLevel,
        context: AnalysisContext
    ): String {
        val specificActions = results
            .filter { it.suggestedAction != null }
            .mapNotNull { it.suggestedAction }
            .distinct()
            .take(3)

        val baseRecommendation = when (threatLevel) {
            ThreatLevel.SAFE -> "No threats detected. You can proceed safely."
            ThreatLevel.SUSPICIOUS -> "Exercise caution. Review the specific concerns below."
            ThreatLevel.LIKELY_MALICIOUS -> "Potential threat detected. Review recommendations carefully before proceeding."
            ThreatLevel.MALICIOUS -> "Threat detected. Follow the specific actions below immediately."
            ThreatLevel.CRITICAL -> "CRITICAL THREAT. Take immediate action as recommended below."
        }

        return if (specificActions.isNotEmpty()) {
            "$baseRecommendation\n\nSpecific Actions:\n${specificActions.mapIndexed { i, action -> "${i + 1}. $action" }.joinToString("\n")}"
        } else {
            baseRecommendation
        }
    }

    private fun buildDecisionDetails(results: List<AgentResult>, decisionScore: Float): Map<String, String> {
        return mapOf(
            "decision_score" to decisionScore.toString(),
            "agents_analyzed" to results.size.toString(),
            "critical_count" to results.count { it.threatLevel == ThreatLevel.CRITICAL }.toString(),
            "malicious_count" to results.count { it.threatLevel == ThreatLevel.MALICIOUS }.toString(),
            "suspicious_count" to results.count { it.threatLevel == ThreatLevel.SUSPICIOUS }.toString(),
            "avg_confidence" to results.map { it.confidence }.average().toString(),
            "max_confidence" to results.maxOfOrNull { it.confidence }.toString()
        )
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No previous analysis results to synthesize"
    )
}
