package com.aegis.agents

import com.aegis.ai.InferenceEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository

class MisinformationAgent(
    private val inferenceEngine: InferenceEngine? = null
) : GuardianAgent {

    override val name = "MisinformationAgent"
    override val version = "1.0.0"
    override val description = "Detects misinformation, fake news, and manipulated content"

    private val misinformationSignals = listOf(
        "breaking news" to 0.1f,
        "they don't want you to know" to 0.3f,
        "share this" to 0.15f,
        "forward this" to 0.15f,
        "send to everyone" to 0.2f,
        "mainstream media won't tell you" to 0.35f,
        "the truth about" to 0.25f,
        "what they're hiding" to 0.3f,
        "government cover up" to 0.35f,
        "big pharma" to 0.25f,
        "miracle cure" to 0.3f,
        "secret remedy" to 0.3f,
        "they don't want you" to 0.25f,
        "wake up sheeple" to 0.4f,
        "do your own research" to 0.2f,
        "100% effective" to 0.25f,
        "guaranteed results" to 0.2f,
        "passed without reading" to 0.3f,
        "emergency alert" to 0.2f,
        "viral" to 0.1f
    )

    private val authoritativeSourcePatterns = listOf(
        Regex("""according to (?:the )?(?:WHO|CDC|FDA|NIH|UN|UNICEF|World Health)""", RegexOption.IGNORE_CASE),
        Regex("""(?:study|research|report) (?:published|from|by) (?:Nature|Science|The Lancet|NEJM|BMJ)""", RegexOption.IGNORE_CASE),
        Regex("""(?:said|stated|confirmed) (?:by|from) (?:Dr\.|Professor|Minister)""", RegexOption.IGNORE_CASE)
    )

    private val emotionalManipulationPatterns = listOf(
        Regex("""\b(?:shocking|outrageous|unbelievable|incredible|mind[ -]?blowing)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:must watch|must read|must see|can't believe|won't believe)\b""", RegexOption.IGNORE_CASE),
        Regex("""!{2,}"""),
        Regex("""\?{2,}""")
    )

    override suspend fun analyze(
        context: AnalysisContext, 
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult {
        val text = context.text ?: return safeResult
        val textLower = text.lowercase()

        val signalScore = misinformationSignals.sumOf { (phrase, weight) ->
            if (textLower.contains(phrase)) weight.toDouble() else 0.0
        }.toFloat()

        val authorityMatches = authoritativeSourcePatterns.count { it.containsMatchIn(text) }
        val emotionalScore = emotionalManipulationPatterns.count { it.containsMatchIn(text) } * 0.15f

        val hasAuthoritySource = authorityMatches > 0
        val mlScore = inferenceEngine?.classify(text, "misinformation") ?: 0f

        // Increased weight for AI/ML analysis (0.7 vs 0.3)
        val combinedScore = if (hasAuthoritySource) {
            (signalScore * 0.2f + emotionalScore * 0.1f + mlScore * 0.7f - 0.2f).coerceIn(0f, 1f)
        } else {
            (signalScore * 0.2f + emotionalScore * 0.1f + mlScore * 0.7f).coerceIn(0f, 1f)
        }

        val threatLevel = scoreToThreatLevel(combinedScore)

        return AgentResult(
            agentName = name,
            threatLevel = threatLevel,
            confidence = combinedScore,
            reason = buildReason(threatLevel, hasAuthoritySource, signalScore),
            details = mapOf(
                "signalScore" to signalScore.toString(),
                "emotionalScore" to emotionalScore.toString(),
                "hasAuthoritySource" to hasAuthoritySource.toString(),
                "mlScore" to mlScore.toString()
            ),
            suggestedAction = when {
                threatLevel.value >= ThreatLevel.MALICIOUS.value -> "This appears to be misinformation. Verify with trusted sources before sharing."
                threatLevel.value >= ThreatLevel.SUSPICIOUS.value -> "Exercise caution — this content shows signs of misinformation."
                else -> null
            },
            requiresUserAttention = threatLevel.value >= ThreatLevel.LIKELY_MALICIOUS.value
        )
    }

    private fun scoreToThreatLevel(score: Float): ThreatLevel = when {
        score >= 0.75f -> ThreatLevel.MALICIOUS
        score >= 0.5f -> ThreatLevel.LIKELY_MALICIOUS
        score >= 0.25f -> ThreatLevel.SUSPICIOUS
        else -> ThreatLevel.SAFE
    }

    private fun buildReason(level: ThreatLevel, hasAuthority: Boolean, signalScore: Float): String = when {
        level.value >= ThreatLevel.MALICIOUS.value -> "Strong misinformation signals detected"
        level.value == ThreatLevel.LIKELY_MALICIOUS.value -> "Content shows multiple misinformation patterns"
        level.value == ThreatLevel.SUSPICIOUS.value -> "Some misinformation indicators found"
        else -> "No misinformation detected"
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No text to analyze"
    )
}
