package com.aegis.agents

import com.aegis.ai.InferenceEngine
import com.aegis.ai.ReasoningEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IntentAgent(
    private val inferenceEngine: InferenceEngine? = null,
    private val reasoningEngine: ReasoningEngine? = null
) : GuardianAgent {

    override val name = "IntentAgent"
    override val version = "2.0.0"
    override val description = "Builds an intent graph to identify psychological manipulation and social engineering"

    private val triggers = mapOf(
        "Urgency" to listOf("urgent", "immediately", "right away", "fast", "limited time", "act now", "deadline"),
        "Authority" to listOf("police", "bank", "manager", "official", "government", "boss", "admin", "security"),
        "Fear" to listOf("arrest", "lawsuit", "suspended", "closed", "blocked", "penalty", "compromised", "illegal"),
        "Financial" to listOf("transfer", "payment", "crypto", "gift card", "refund", "win", "lottery", "investment"),
        "Isolation" to listOf("don't tell", "keep secret", "private", "only you", "stay quiet", "confidential")
    )

    override suspend fun analyze(
        context: AnalysisContext, 
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult =
        withContext(Dispatchers.Default) {
            val text = context.text ?: return@withContext safeResult
            val textLower = text.lowercase()

            val detectedTriggers = triggers.filter { (_, keywords) ->
                keywords.any { textLower.contains(it) }
            }.keys.toList()

            val baseIntentScore = (detectedTriggers.size * 0.2f).coerceIn(0f, 1f)
            
            // ML-based intent classification
            val mlScore = if (context.precomputedAiResult != null) {
                if (context.precomputedAiResult.threatType == "impersonation" || 
                    context.precomputedAiResult.threatType == "social_engineering") 0.8f else 0.2f
            } else {
                inferenceEngine?.classify(text, "intent_analysis", context.metadata) ?: 0f
            }
            
            // Deep reasoning for complex manipulation if needed
            var finalScore = (baseIntentScore * 0.6f + mlScore * 0.4f)
            var manipulationDetails = ""

            if (finalScore >= 0.4f && reasoningEngine != null && context.conversationHistory.isNotEmpty()) {
                val deepReasoning = if (context.precomputedAiResult != null) {
                    context.precomputedAiResult.reason
                } else {
                    reasoningEngine.analyzeConversation(context.conversationHistory, text, context.metadata)
                }

                manipulationDetails = deepReasoning
                // If deep reasoning confirms high risk, increase score
                if (deepReasoning.contains("high risk", ignoreCase = true) || 
                    deepReasoning.contains("9", ignoreCase = true)) {
                    finalScore = finalScore.coerceAtLeast(0.8f)
                }
            }

            val threatLevel = scoreToThreatLevel(finalScore)
            
            val reason = when {
                manipulationDetails.isNotEmpty() -> manipulationDetails
                detectedTriggers.isNotEmpty() -> "Detected manipulation triggers: ${detectedTriggers.joinToString(" → ")}"
                else -> buildReason(threatLevel, finalScore)
            }

            AgentResult(
                agentName = name,
                threatLevel = threatLevel,
                confidence = finalScore,
                reason = reason,
                details = mapOf(
                    "triggers" to detectedTriggers.joinToString(","),
                    "trigger_count" to detectedTriggers.size.toString(),
                    "ml_intent_score" to mlScore.toString(),
                    "deep_analysis" to manipulationDetails
                ),
                suggestedAction = getAdvice(detectedTriggers, threatLevel)
            )
        }

    private fun getAdvice(triggers: List<String>, level: ThreatLevel): String? {
        if (level == ThreatLevel.SAFE) return null
        
        return when {
            triggers.contains("Authority") && triggers.contains("Urgency") -> 
                "Real authorities will never pressure you to act immediately over text. Stop and verify."
            triggers.contains("Financial") && triggers.contains("Isolation") ->
                "Requests for money combined with secrecy are classic scam indicators. Consult someone you trust."
            else -> "Evaluate if the sender's request aligns with their stated identity and common practices."
        }
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
