package com.aegis.ai

/**
 * Data class representing the result of a threat analysis by Gemini AI.
 */
data class ThreatAnalysisResult(
    val isThreat: Boolean,
    val threatType: String,
    val confidence: Float,
    val reason: String,
    val guidance: String,
    val appContext: String?
)

/**
 * Data class representing the result of a conversation analysis by Gemini AI.
 */
data class ConversationAnalysisResult(
    val isSuspicious: Boolean,
    val threatType: String?,
    val confidence: Float,
    val analysis: String,
    val recommendedActions: List<String>,
    val riskFactors: List<String>
)
