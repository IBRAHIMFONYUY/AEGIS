package com.aegis.agents

import com.aegis.ai.InferenceEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IntentAgent(
    private val inferenceEngine: InferenceEngine? = null
) : GuardianAgent {

    override val name = "IntentAgent"
    override val version = "1.0.0"
    override val description = "Analyzes the underlying intent and ethics of digital interactions"

    private val maliciousIntents = listOf(
        "extortion", "manipulation", "impersonation", "coercion", "deception"
    )

    override suspend fun analyze(
        context: AnalysisContext, 
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult =
        withContext(Dispatchers.Default) {
            val text = context.text ?: return@withContext safeResult
            
            val intentScore = analyzeIntent(text)
            val threatLevel = scoreToThreatLevel(intentScore)
            
            AgentResult(
                agentName = name,
                threatLevel = threatLevel,
                confidence = intentScore,
                reason = buildReason(threatLevel, intentScore),
                suggestedAction = if (threatLevel.value >= ThreatLevel.SUSPICIOUS.value) 
                    "Evaluate if the sender's request aligns with their stated identity." else null
            )
        }

    private fun analyzeIntent(text: String): Float {
        val textLower = text.lowercase()
        var score = 0f
        
        if (textLower.contains("don't tell anyone") || textLower.contains("keep this secret")) {
            score += 0.4f // Indicators of isolation/manipulation
        }
        
        if (textLower.contains("gift card") || textLower.contains("crypto") || textLower.contains("transfer")) {
            if (textLower.contains("urgent") || textLower.contains("fast")) {
                score += 0.5f // Financial coercion
            }
        }

        if (textLower.contains("i am your") || textLower.contains("this is your boss")) {
            score += 0.3f // Authority impersonation
        }

        return score.coerceIn(0f, 1f)
    }

    private fun scoreToThreatLevel(score: Float): ThreatLevel = when {
        score >= 0.7f -> ThreatLevel.MALICIOUS
        score >= 0.4f -> ThreatLevel.SUSPICIOUS
        else -> ThreatLevel.SAFE
    }

    private fun buildReason(level: ThreatLevel, score: Float): String = when (level) {
        ThreatLevel.SAFE -> "No malicious intent detected"
        ThreatLevel.SUSPICIOUS -> "Potential manipulative intent detected"
        ThreatLevel.MALICIOUS -> "High probability of malicious intent (Coercion/Manipulation)"
        else -> "Analysis inconclusive"
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No data"
    )
}
