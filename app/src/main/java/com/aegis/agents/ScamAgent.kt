package com.aegis.agents

import com.aegis.ai.InferenceEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import com.aegis.network.ThreatIntelClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScamAgent(
    private val inferenceEngine: InferenceEngine? = null,
    private val threatIntelClient: ThreatIntelClient? = null,
    private val gemmaEngine: com.aegis.ai.GemmaInferenceEngine? = null
) : GuardianAgent {

    override val name = "ScamAgent"
    override val version = "2.0.0"
    override val description = "Detects phishing, fraud, and scam attempts in text and URLs using AI"

    private val scamKeywords = listOf(
        "urgent", "bank", "account", "verify", "password", "credit card",
        "social security", "ssn", "win", "prize", "lottery", "inheritance",
        "money gram", "western union", "wire transfer", "gift card",
        "bitcoin", "crypto", "invest", "guaranteed", "risk free",
        "click here", "limited time", "act now", "suspended",
        "unusual activity", "login", "confirm your identity",
        "tax refund", "irs", "government grant", "nigerian prince",
        "refund", "payment", "overdue", "collection"
    )

    private val scamPatterns = listOf(
        Regex("""\b(?:urgent|immediately|right away)\b.*\b(?:action|click|call|send|pay)\b""",
            RegexOption.IGNORE_CASE),
        Regex("""\b(?:bank|account|credit|debit|card)\b.*\b(?:suspend|close|block|verify|confirm)\b""",
            RegexOption.IGNORE_CASE),
        Regex("""\b(?:won|winner|selected|chosen)\b.*\b(?:prize|lottery|raffle|draw)\b""",
            RegexOption.IGNORE_CASE),
        Regex("""\b(?:wire|send|transfer|pay)\b.*\b(?:money|fund|cash|fee|tax)\b""",
            RegexOption.IGNORE_CASE),
        Regex("""https?://(?:bit\.ly|tinyurl|ow\.ly|is\.gd|buff\.ly|rb\.gy)\S+""",
            RegexOption.IGNORE_CASE)
    )

    override suspend fun analyze(
        context: AnalysisContext, 
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult =
        withContext(Dispatchers.Default) {
            val text = context.text ?: return@withContext safeResult
            val url = context.url

            val ruleBasedScore = ruleBasedAnalysis(text, url)
            val mlScore = inferenceEngine?.let { mlAnalysis(text, context.metadata) } ?: 0f
            
            var cloudScore = 0f
            if (url != null) {
                cloudScore = threatIntelClient?.checkUrl(url)?.let { if (it.isMalicious) it.confidence else 0f } ?: 0f
            }

            // Context Engine additions
            var contextMultiplier = 1.0f
            if (context.isUnknownSender) contextMultiplier += 0.3f
            if (context.appRiskScore > 0.5f) contextMultiplier += 0.2f
            if (context.sourceType == SourceType.SMS) contextMultiplier += 0.1f

            val ignoreCount = memory?.getLatest("GLOBAL_IGNORE_COUNT")?.toIntOrNull() ?: 0
            val behavioralMultiplier = if (ignoreCount > 3) 1.2f else 1.0f

            // Increased weight for AI/ML analysis for higher accuracy
            // Combining Rule (20%), ML (50%), Cloud (30%)
            val combinedScore = (ruleBasedScore * 0.2f + mlScore * 0.5f + cloudScore * 0.3f) * behavioralMultiplier * contextMultiplier
            val threatLevel = scoreToThreatLevel(combinedScore)
            val reason = buildReason(threatLevel, combinedScore, ignoreCount > 3, context.isUnknownSender)

            val dialogueShiftAdvice = getDialogueShiftAdvice(context)
            val baseSuggestedAction = suggestedAction(threatLevel, ignoreCount > 3)
            val finalAction = if (dialogueShiftAdvice != null) "$baseSuggestedAction\n\n$dialogueShiftAdvice" else baseSuggestedAction

            AgentResult(
                agentName = name,
                threatLevel = threatLevel,
                confidence = combinedScore.coerceIn(0f, 1f),
                reason = reason,
                details = mapOf(
                    "ruleScore" to ruleBasedScore.toString(),
                    "mlScore" to mlScore.toString(),
                    "cloudScore" to cloudScore.toString(),
                    "contextMultiplier" to contextMultiplier.toString(),
                    "behavioralMultiplier" to behavioralMultiplier.toString(),
                    "isUnknownSender" to context.isUnknownSender.toString(),
                    "chat_history_size" to context.conversationHistory.size.toString()
                ),
                suggestedAction = finalAction,
                requiresUserAttention = threatLevel.value >= ThreatLevel.MALICIOUS.value
            )
        }

    private fun ruleBasedAnalysis(text: String, url: String?): Float {
        var score = 0f
        val textLower = text.lowercase()

        val matched = scamKeywords.count { textLower.contains(it) }
        score += matched * 0.1f

        val patternMatches = scamPatterns.count { it.containsMatchIn(text) }
        score += patternMatches * 0.15f

        if (url != null) {
            if (!url.startsWith("https://")) score += 0.1f
            if (url.count { it == '.' } > 3) score += 0.1f
            if (url.contains("login") || url.contains("verify") || url.contains("secure")) score += 0.1f
        }

        if (text.length < 20 && matched > 2) score += 0.2f
        val exclamationCount = text.count { it == '!' }
        if (exclamationCount > 3) score += 0.1f

        return score.coerceIn(0f, 1f)
    }

    private suspend fun mlAnalysis(text: String, metadata: Map<String, String> = emptyMap()): Float {
        // Try Gemini AI first for better accuracy
        if (gemmaEngine != null) {
            val threatAnalysis = gemmaEngine.detectThreatWithAI(text, metadata)
            return if (threatAnalysis.isThreat && threatAnalysis.threatType == "scam") {
                threatAnalysis.confidence
            } else {
                0f
            }
        }
        return inferenceEngine?.classify(text, "scam_detection") ?: 0f
    }
    
    suspend fun analyzeWithRealInference(
        context: AnalysisContext,
        memory: GuardianMemoryRepository?,
        gemmaEngine: com.aegis.ai.GemmaInferenceEngine?
    ): AgentResult =
        withContext(Dispatchers.Default) {
            val text = context.text ?: return@withContext safeResult
            val url = context.url

            val ruleBasedScore = ruleBasedAnalysis(text, url)
            
            // Use Gemma specialized task method for real ML analysis
            val mlScore = gemmaEngine?.detectScam(text, context.metadata) ?: 0f
            
            var cloudScore = 0f
            if (url != null) {
                cloudScore = threatIntelClient?.checkUrl(url)?.let { if (it.isMalicious) it.confidence else 0f } ?: 0f
            }

            // Context Engine additions
            var contextMultiplier = 1.0f
            if (context.isUnknownSender) contextMultiplier += 0.3f
            if (context.appRiskScore > 0.5f) contextMultiplier += 0.2f
            if (context.sourceType == SourceType.SMS) contextMultiplier += 0.1f

            val ignoreCount = memory?.getLatest("GLOBAL_IGNORE_COUNT")?.toIntOrNull() ?: 0
            val behavioralMultiplier = if (ignoreCount > 3) 1.2f else 1.0f

            // Increased weight for AI/ML analysis for higher accuracy
            // Combining Rule (20%), ML (50%), Cloud (30%)
            val combinedScore = (ruleBasedScore * 0.2f + mlScore * 0.5f + cloudScore * 0.3f) * behavioralMultiplier * contextMultiplier
            val threatLevel = scoreToThreatLevel(combinedScore)
            val reason = buildReason(threatLevel, combinedScore, ignoreCount > 3, context.isUnknownSender)

            val dialogueShiftAdvice = getDialogueShiftAdvice(context)
            val baseSuggestedAction = suggestedAction(threatLevel, ignoreCount > 3)
            val finalAction = if (dialogueShiftAdvice != null) "$baseSuggestedAction\n\n$dialogueShiftAdvice" else baseSuggestedAction

            AgentResult(
                agentName = name,
                threatLevel = threatLevel,
                confidence = combinedScore.coerceIn(0f, 1f),
                reason = reason,
                details = mapOf(
                    "ruleScore" to ruleBasedScore.toString(),
                    "mlScore" to mlScore.toString(),
                    "cloudScore" to cloudScore.toString(),
                    "contextMultiplier" to contextMultiplier.toString(),
                    "behavioralMultiplier" to behavioralMultiplier.toString(),
                    "isUnknownSender" to context.isUnknownSender.toString(),
                    "chat_history_size" to context.conversationHistory.size.toString(),
                    "inferenceEngine" to (gemmaEngine?.let { "Gemma 3N" } ?: "None")
                ),
                suggestedAction = finalAction,
                requiresUserAttention = threatLevel.value >= ThreatLevel.MALICIOUS.value
            )
        }

    private fun matchedKeywords(text: String): List<String> {
        val textLower = text.lowercase()
        return scamKeywords.filter { textLower.contains(it) }
    }

    private fun scoreToThreatLevel(score: Float): ThreatLevel = when {
        score >= 0.8f -> ThreatLevel.CRITICAL
        score >= 0.6f -> ThreatLevel.MALICIOUS
        score >= 0.4f -> ThreatLevel.LIKELY_MALICIOUS
        score >= 0.2f -> ThreatLevel.SUSPICIOUS
        else -> ThreatLevel.SAFE
    }

    private fun buildReason(level: ThreatLevel, score: Float, isHighRiskBehavior: Boolean, isUnknownSender: Boolean): String {
        val baseReason = when (level) {
            ThreatLevel.SAFE -> "No scam indicators detected"
            ThreatLevel.SUSPICIOUS -> "Some scam-like patterns found (${(score * 100).toInt()}% confidence)"
            ThreatLevel.LIKELY_MALICIOUS -> "Multiple scam indicators detected"
            ThreatLevel.MALICIOUS -> "Strong scam signature detected — high probability of phishing or fraud"
            ThreatLevel.CRITICAL -> "Critical scam threat detected — immediate attention recommended"
        }
        
        val contextInfo = if (isUnknownSender && level.value >= ThreatLevel.SUSPICIOUS.value) {
            " from an unknown sender"
        } else ""

        return if (isHighRiskBehavior && level.value >= ThreatLevel.SUSPICIOUS.value) {
            "⚠ $baseReason$contextInfo (Sensitivity increased due to recent ignored alerts)"
        } else {
            "$baseReason$contextInfo"
        }
    }

    private fun suggestedAction(level: ThreatLevel, isHighRiskBehavior: Boolean): String? {
        val baseAction = when (level) {
            ThreatLevel.SAFE -> null
            ThreatLevel.SUSPICIOUS -> "Exercise caution. Don't share personal information."
            ThreatLevel.LIKELY_MALICIOUS -> "Do not respond. Do not click any links. Report as spam."
            ThreatLevel.MALICIOUS -> "Block sender. Do not engage. Report to authorities."
            ThreatLevel.CRITICAL -> "Immediate action required. Do not interact. Report to cybersecurity team."
        }
        return if (isHighRiskBehavior && baseAction != null) {
            "$baseAction (Please take this seriously based on your recent activity)"
        } else {
            baseAction
        }
    }

    private fun getDialogueShiftAdvice(context: AnalysisContext): String? {
        if (context.conversationHistory.size < 3) return null
        
        // Analyze for sudden shifts (e.g., normal chat -> urgent financial request)
        val historyText = context.conversationHistory.joinToString(" ").lowercase()
        val currentText = context.text?.lowercase() ?: ""
        
        val historyHadMoney = historyText.contains("money") || historyText.contains("pay") || historyText.contains("momo")
        val currentHasMoney = currentText.contains("money") || currentText.contains("pay") || currentText.contains("momo")
        
        if (!historyHadMoney && currentHasMoney && currentText.contains("urgent")) {
            return "Note: This contact has suddenly shifted to an urgent financial request, which is a high-risk behavior pattern."
        }
        
        return null
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No text to analyze"
    )
}
