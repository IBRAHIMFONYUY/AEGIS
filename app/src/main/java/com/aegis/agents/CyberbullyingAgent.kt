package com.aegis.agents

import com.aegis.ai.InferenceEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository

class CyberbullyingAgent(
    private val inferenceEngine: InferenceEngine? = null
) : GuardianAgent {

    override val name = "CyberbullyingAgent"
    override val version = "1.0.0"
    override val description = "Detects cyberbullying, harassment, and toxic language"

    private val bullyingPatterns = mapOf(
        "insults" to listOf(
            "stupid", "idiot", "dumb", "moron", "loser", "worthless", "useless",
            "retard", "ignorant", "pathetic", "trash", "garbage"
        ),
        "threats" to listOf(
            "kill you", "hurt you", "watch your back", "you're dead", "gonna get you",
            "end you", "destroy you", "shut you up", "beat you"
        ),
        "dismissive" to listOf(
            "nobody likes you", "no one wants you", "go away", "kill yourself",
            "disappear", "nobody cares", "just die", "end your life"
        ),
        "sexual_harassment" to listOf(
            "send nudes", "sexy", "slut", "whore", "bitch", "hoe",
            "fuck you", "suck my", "blowjob"
        ),
        "appearance_based" to listOf(
            "ugly", "fat", "disgusting", "hideous", "gross", "nasty",
            "four eyes", "shorty", "midget", "skinny"
        ),
        "exclusion" to listOf(
            "you don't belong", "not welcome", "no one wants you here",
            "leave us alone", "stay away", "ignore"
        )
    )

    override suspend fun analyze(
        context: AnalysisContext, 
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult {
        val text = context.text ?: return safeResult
        val textLower = text.lowercase()

        val matchedCategories = mutableMapOf<String, List<String>>()
        for ((category, phrases) in bullyingPatterns) {
            val found = phrases.filter { textLower.contains(it) }
            if (found.isNotEmpty()) {
                matchedCategories[category] = found
            }
        }

        val mlScore = inferenceEngine?.classify(text, "toxicity") ?: 0f

        val patternScore = computeScore(matchedCategories)
        // Increased weight for AI/ML analysis (0.8 vs 0.2)
        val combinedScore = (patternScore * 0.2f + mlScore * 0.8f)
        val threatLevel = scoreToThreatLevel(combinedScore)

        val categoryLabels = matchedCategories.keys.joinToString(", ")
        val severity = assessSeverity(matchedCategories)

        return AgentResult(
            agentName = name,
            threatLevel = threatLevel,
            confidence = combinedScore.coerceIn(0f, 1f),
            reason = buildReason(threatLevel, severity, matchedCategories),
            details = mapOf(
                "categories" to categoryLabels,
                "severity" to severity,
                "patternScore" to patternScore.toString(),
                "mlScore" to mlScore.toString(),
                "matchedTerms" to matchedCategories.values.flatten().joinToString(",")
            ),
            suggestedAction = when {
                severity == "severe" -> "This contains serious threats. Consider reporting to authorities."
                severity == "moderate" -> "Cyberbullying detected. Block sender and report the content."
                severity == "mild" -> "This may be cyberbullying. Consider addressing it or ignoring."
                else -> null
            },
            requiresUserAttention = threatLevel.value >= ThreatLevel.LIKELY_MALICIOUS.value
        )
    }

    private fun computeScore(categories: Map<String, List<String>>): Float {
        if (categories.isEmpty()) return 0f
        var score = 0f
        for ((category, terms) in categories) {
            score += when (category) {
                "threats" -> 0.3f * terms.size
                "dismissive" -> 0.25f * terms.size
                "sexual_harassment" -> 0.25f * terms.size
                "insults" -> 0.15f * terms.size
                "appearance_based" -> 0.15f * terms.size
                "exclusion" -> 0.2f * terms.size
                else -> 0.1f * terms.size
            }
        }
        return score.coerceIn(0f, 1f)
    }

    private fun assessSeverity(categories: Map<String, List<String>>): String {
        if (categories.containsKey("threats") || categories.containsKey("dismissive")) return "severe"
        if (categories.containsKey("sexual_harassment")) return "severe"
        if (categories.size >= 3) return "moderate"
        if (categories.isNotEmpty()) return "mild"
        return "none"
    }

    private fun scoreToThreatLevel(score: Float): ThreatLevel = when {
        score >= 0.7f -> ThreatLevel.MALICIOUS
        score >= 0.5f -> ThreatLevel.LIKELY_MALICIOUS
        score >= 0.25f -> ThreatLevel.SUSPICIOUS
        else -> ThreatLevel.SAFE
    }

    private fun buildReason(level: ThreatLevel, severity: String, categories: Map<String, List<String>>): String = when {
        severity == "severe" -> "Severe cyberbullying detected with threats or harmful content"
        severity == "moderate" -> "Multiple forms of cyberbullying detected"
        level.value >= ThreatLevel.SUSPICIOUS.value -> "Potential bullying language detected"
        else -> "No bullying detected"
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No text to analyze"
    )
}
