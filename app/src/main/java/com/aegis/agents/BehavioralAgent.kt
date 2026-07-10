package com.aegis.agents

import com.aegis.ai.InferenceEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BehavioralAgent(
    private val inferenceEngine: InferenceEngine? = null,
    private val memory: GuardianMemoryRepository? = null
) : GuardianAgent {

    override val name = "BehavioralAgent"
    override val version = "1.0.0"
    override val description = "Analyzes user behavior patterns to detect anomalies and potential compromise"

    private val baselineWindow = 50 // Number of interactions to establish baseline
    private val anomalyThreshold = 2.0f // Standard deviations from baseline

    override suspend fun analyze(
        context: AnalysisContext,
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult =
        withContext(Dispatchers.Default) {
            val text = context.text ?: return@withContext safeResult
            val chatId = context.metadata["chat_id"] ?: context.sourceApp

            // Analyze current interaction
            val currentBehavior = analyzeCurrentBehavior(context, text)
            
            // Compare with historical baseline
            val baseline = getBehavioralBaseline(chatId, memory)
            val anomalyScore = calculateAnomalyScore(currentBehavior, baseline)
            
            // ML-based behavioral analysis using task-specific AI
            val mlScore = if (inferenceEngine is com.aegis.ai.GemmaInferenceEngine && context.conversationHistory.isNotEmpty()) {
                val summary = inferenceEngine.summarizeConversation(context.conversationHistory, context.metadata)
                // Use summary to adjust score (heuristic for now)
                if (summary.contains("suspicious", ignoreCase = true)) 0.6f else 0.2f
            } else {
                inferenceEngine?.classify(text, "behavioral_analysis", context.metadata) ?: 0f
            }

            // Combine anomaly detection with ML
            val combinedScore = (anomalyScore * 0.6f + mlScore * 0.4f).coerceIn(0f, 1f)
            val threatLevel = scoreToThreatLevel(combinedScore)

            // Store current behavior for future baseline
            memory?.storeBehavioralData(
                chatId = chatId,
                behaviorMetrics = currentBehavior,
                timestamp = System.currentTimeMillis()
            )

            val reason = buildReason(threatLevel, combinedScore, currentBehavior, baseline)
            val details = buildDetails(currentBehavior, baseline, anomalyScore, mlScore)

            AgentResult(
                agentName = name,
                threatLevel = threatLevel,
                confidence = combinedScore,
                reason = reason,
                details = details,
                suggestedAction = getSuggestedAction(threatLevel, currentBehavior),
                requiresUserAttention = threatLevel.value >= ThreatLevel.MALICIOUS.value
            )
        }

    private fun analyzeCurrentBehavior(context: AnalysisContext, text: String): BehavioralMetrics {
        val textLength = text.length
        val urgencyLevel = calculateUrgency(text)
        val financialMention = text.contains(Regex("""(?i)(money|pay|transfer|crypto|gift card|refund|win|lottery)"""))
        val personalInfoRequest = text.contains(Regex("""(?i)(password|ssn|social security|credit card|bank account|otp|pin)"""))
        val unusualTime = isUnusualTime()
        val responseSpeed = context.metadata["response_time"]?.toFloatOrNull() ?: 0f

        return BehavioralMetrics(
            textLength = textLength,
            urgencyLevel = urgencyLevel,
            financialMention = financialMention,
            personalInfoRequest = personalInfoRequest,
            unusualTime = unusualTime,
            responseSpeed = responseSpeed
        )
    }

    private fun calculateUrgency(text: String): Float {
        val urgencyKeywords = listOf("urgent", "immediately", "right away", "fast", "now", "quickly", "asap")
        val textLower = text.lowercase()
        val matches = urgencyKeywords.count { textLower.contains(it) }
        return (matches * 0.2f).coerceIn(0f, 1f)
    }

    private fun isUnusualTime(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        // Consider late night (10 PM - 6 AM) as unusual for financial/personal requests
        return hour in 22..23 || hour in 0..5
    }

    private suspend fun getBehavioralBaseline(chatId: String?, memory: GuardianMemoryRepository?): BehavioralBaseline? {
        if (chatId == null || memory == null) return null

        val history = memory.getBehavioralHistory(chatId, limit = baselineWindow)
        if (history.size < 10) return null // Not enough data for baseline

        return BehavioralBaseline(
            avgTextLength = history.map { it.textLength }.average().toFloat(),
            avgUrgency = history.map { it.urgencyLevel }.average().toFloat(),
            financialFrequency = history.count { it.financialMention }.toFloat() / history.size,
            personalInfoFrequency = history.count { it.personalInfoRequest }.toFloat() / history.size,
            unusualTimeFrequency = history.count { it.unusualTime }.toFloat() / history.size,
            avgResponseSpeed = history.map { it.responseSpeed }.average().toFloat()
        )
    }

    private fun calculateAnomalyScore(current: BehavioralMetrics, baseline: BehavioralBaseline?): Float {
        if (baseline == null) return 0f

        var anomalyScore = 0f

        // Text length anomaly
        val lengthDiff = kotlin.math.abs(current.textLength - baseline.avgTextLength) / baseline.avgTextLength
        if (lengthDiff > 2.0f) anomalyScore += 0.2f

        // Urgency anomaly
        if (current.urgencyLevel > baseline.avgUrgency * 2) anomalyScore += 0.3f

        // Financial mention anomaly
        if (current.financialMention && baseline.financialFrequency < 0.1f) anomalyScore += 0.25f

        // Personal info request anomaly
        if (current.personalInfoRequest && baseline.personalInfoFrequency < 0.05f) anomalyScore += 0.35f

        // Unusual time anomaly
        if (current.unusualTime && baseline.unusualTimeFrequency < 0.2f) anomalyScore += 0.15f

        return anomalyScore.coerceIn(0f, 1f)
    }

    private fun scoreToThreatLevel(score: Float): ThreatLevel = when {
        score >= 0.7f -> ThreatLevel.CRITICAL
        score >= 0.5f -> ThreatLevel.MALICIOUS
        score >= 0.3f -> ThreatLevel.LIKELY_MALICIOUS
        score >= 0.15f -> ThreatLevel.SUSPICIOUS
        else -> ThreatLevel.SAFE
    }

    private fun buildReason(level: ThreatLevel, score: Float, current: BehavioralMetrics, baseline: BehavioralBaseline?): String {
        if (baseline == null) {
            return "Building behavioral baseline - insufficient historical data"
        }

        val anomalies = mutableListOf<String>()
        
        if (current.urgencyLevel > baseline.avgUrgency * 2) anomalies.add("unusual urgency")
        if (current.financialMention && baseline.financialFrequency < 0.1f) anomalies.add("unexpected financial request")
        if (current.personalInfoRequest && baseline.personalInfoFrequency < 0.05f) anomalies.add("unusual personal information request")
        if (current.unusualTime && baseline.unusualTimeFrequency < 0.2f) anomalies.add("unusual timing")

        return when (level) {
            ThreatLevel.SAFE -> "Behavior matches established patterns"
            ThreatLevel.SUSPICIOUS -> "Minor behavioral deviation detected: ${anomalies.joinToString(", ")}"
            ThreatLevel.LIKELY_MALICIOUS -> "Behavioral anomalies detected: ${anomalies.joinToString(", ")}"
            ThreatLevel.MALICIOUS -> "Significant behavioral deviation - potential account compromise or impersonation"
            ThreatLevel.CRITICAL -> "Critical behavioral anomaly - immediate verification recommended"
        }
    }

    private fun buildDetails(current: BehavioralMetrics, baseline: BehavioralBaseline?, anomalyScore: Float, mlScore: Float): Map<String, String> {
        return mapOf(
            "current_urgency" to current.urgencyLevel.toString(),
            "current_financial" to current.financialMention.toString(),
            "current_personal_info" to current.personalInfoRequest.toString(),
            "current_unusual_time" to current.unusualTime.toString(),
            "baseline_avg_urgency" to (baseline?.avgUrgency ?: 0f).toString(),
            "baseline_financial_freq" to (baseline?.financialFrequency ?: 0f).toString(),
            "baseline_personal_info_freq" to (baseline?.personalInfoFrequency ?: 0f).toString(),
            "anomaly_score" to anomalyScore.toString(),
            "ml_score" to mlScore.toString()
        )
    }

    private fun getSuggestedAction(level: ThreatLevel, current: BehavioralMetrics): String? {
        return when (level) {
            ThreatLevel.SAFE -> null
            ThreatLevel.SUSPICIOUS -> "This interaction deviates from normal patterns. Proceed with caution."
            ThreatLevel.LIKELY_MALICIOUS -> "Behavioral anomaly detected. Verify the sender's identity through another channel."
            ThreatLevel.MALICIOUS -> "Significant behavioral deviation detected. This may be an impersonation attempt."
            ThreatLevel.CRITICAL -> "Critical: Account may be compromised. Enable 2FA and change passwords immediately."
        }
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No behavior to analyze"
    )
}

data class BehavioralMetrics(
    val textLength: Int,
    val urgencyLevel: Float,
    val financialMention: Boolean,
    val personalInfoRequest: Boolean,
    val unusualTime: Boolean,
    val responseSpeed: Float
)

data class BehavioralBaseline(
    val avgTextLength: Float,
    val avgUrgency: Float,
    val financialFrequency: Float,
    val personalInfoFrequency: Float,
    val unusualTimeFrequency: Float,
    val avgResponseSpeed: Float
)
