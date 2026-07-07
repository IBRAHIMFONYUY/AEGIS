package com.aegis.agents

import com.aegis.ai.InferenceEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository

class PrivacyAgent(
    private val inferenceEngine: InferenceEngine? = null
) : GuardianAgent {

    override val name = "PrivacyAgent"
    override val version = "1.0.0"
    override val description = "Detects privacy-invasive content, data leakage, and sensitive information exposure"

    private val sensitivePatterns = mapOf(
        "Email" to Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
        "Phone" to Regex("""\+?\d{1,3}[-.\s]?\(?\d{1,4}\)?[-.\s]?\d{1,4}[-.\s]?\d{1,9}"""),
        "CreditCard" to Regex("""\b(?:\d[ -]*?){13,16}\b"""),
        "SSN" to Regex("""\b\d{3}[-]?\d{2}[-]?\d{4}\b"""),
        "BankAccount" to Regex("""\b\d{8,17}\b"""),
        "IPAddress" to Regex("""\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b"""),
        "Password" to Regex("""\b(?:password|passwd|pwd|secret)\s*[:=]\s*\S+""", RegexOption.IGNORE_CASE),
        "APIKey" to Regex("""(?:api[_-]?key|apikey|api[_-]?secret)\s*[:=]\s*\S+""", RegexOption.IGNORE_CASE),
        "Address" to Regex("""\d+\s+[A-Za-z\s]+\b(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr|Way|Court|Ct)\b""", RegexOption.IGNORE_CASE)
    )

    private val privacyInvasivePhrases = listOf(
        "track your", "monitor your", "collect your data", "share with third parties",
        "sell your information", "access your", "read your messages", "read your contacts",
        "record your", "listen to", "view your photos", "access your camera",
        "access your location", "background location", "access your microphone",
        "upload your contacts", "sync your data", "read your call log",
        "access your calendar", "modify your settings"
    )

    override suspend fun analyze(
        context: AnalysisContext, 
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult {
        val text = context.text ?: return safeResult

        if (context.metadata["is_permission_dialog"] == "true") {
            return analyzePermissionRequest(context)
        }

        val foundSensitive = mutableMapOf<String, Int>()
        for ((type, pattern) in sensitivePatterns) {
            val matches = pattern.findAll(text).toList()
            if (matches.isNotEmpty()) {
                foundSensitive[type] = matches.size
            }
        }

        val textLower = text.lowercase()
        val intrusivePhrases = privacyInvasivePhrases.filter { textLower.contains(it) }

        val sensitivityScore = computeScore(foundSensitive, intrusivePhrases)
        val threatLevel = scoreToThreatLevel(sensitivityScore)

        return AgentResult(
            agentName = name,
            threatLevel = threatLevel,
            confidence = sensitivityScore.coerceIn(0f, 1f),
            reason = buildReason(threatLevel, foundSensitive.keys, intrusivePhrases),
            details = mapOf(
                "sensitiveDataTypes" to foundSensitive.keys.joinToString(","),
                "intrusivePhrases" to intrusivePhrases.joinToString(","),
                "sensitivityScore" to sensitivityScore.toString()
            ),
            suggestedAction = when {
                foundSensitive.containsKey("CreditCard") -> "Credit card number detected. Do not share this."
                foundSensitive.containsKey("SSN") -> "Social Security number detected. This is highly sensitive."
                intrusivePhrases.isNotEmpty() -> "This may be a privacy-invasive request."
                else -> null
            },
            requiresUserAttention = threatLevel.value >= ThreatLevel.LIKELY_MALICIOUS.value
        )
    }

    private fun computeScore(sensitive: Map<String, Int>, intrusive: List<String>): Float {
        var score = 0f
        sensitive.forEach { (type, count) ->
            score += when (type) {
                "CreditCard", "SSN", "BankAccount" -> 0.25f * count
                "Password", "APIKey" -> 0.2f * count
                "Email", "Phone" -> 0.1f * count
                "Address" -> 0.1f * count
                else -> 0.05f * count
            }
        }
        score += intrusive.size * 0.15f
        return score.coerceIn(0f, 1f)
    }

    private fun scoreToThreatLevel(score: Float): ThreatLevel = when {
        score >= 0.8f -> ThreatLevel.CRITICAL
        score >= 0.5f -> ThreatLevel.MALICIOUS
        score >= 0.3f -> ThreatLevel.LIKELY_MALICIOUS
        score >= 0.1f -> ThreatLevel.SUSPICIOUS
        else -> ThreatLevel.SAFE
    }

    private fun buildReason(level: ThreatLevel, types: Set<String>, phrases: List<String>): String = when {
        types.contains("CreditCard") -> "Exposed credit card number detected"
        types.contains("SSN") -> "Social Security number detected in content"
        level.value >= ThreatLevel.MALICIOUS.value -> "Significant personal data exposure detected"
        level.value == ThreatLevel.LIKELY_MALICIOUS.value -> "Personal information detected"
        level.value == ThreatLevel.SUSPICIOUS.value -> "Potential privacy concern detected"
        else -> "No privacy concerns detected"
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No text to analyze"
    )
}
