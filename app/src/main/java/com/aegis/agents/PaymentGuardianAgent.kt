package com.aegis.agents

import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaymentGuardianAgent : GuardianAgent {

    override val name = "PaymentGuardian"
    override val version = "1.0.0"
    override val description = "Monitors financial transactions and payment requests for safety"

    private val riskyPaymentKeywords = listOf(
        "payment", "invoice", "confirm", "otp", "verify", "code", "transaction",
        "mobile money", "momo", "orange money", "transfer", "send money"
    )

    override suspend fun analyze(
        context: AnalysisContext,
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult = withContext(Dispatchers.Default) {
        val text = context.text?.lowercase() ?: ""
        val sourceApp = context.sourceApp?.lowercase() ?: ""
        
        val isFinancialApp = sourceApp.contains("bank") || 
                           sourceApp.contains("payment") || 
                           sourceApp.contains("wallet") ||
                           sourceApp.contains("momo")
        
        val hasFinancialKeywords = riskyPaymentKeywords.any { text.contains(it) }
        
        if (!isFinancialApp && !hasFinancialKeywords) return@withContext safeResult

        var riskScore = 0f
        val details = mutableMapOf<String, String>()

        if (context.isUnknownSender && hasFinancialKeywords) {
            riskScore += 0.6f
            details["unknown_sender_financial"] = "true"
        }

        if (context.url != null) {
            if (!context.url.startsWith("https://")) {
                riskScore += 0.3f
                details["insecure_payment_url"] = "true"
            }
        }

        if (text.contains("otp") || text.contains("verification code")) {
            riskScore += 0.4f
            details["otp_request"] = "true"
        }

        val threatLevel = scoreToThreatLevel(riskScore)
        
        AgentResult(
            agentName = name,
            threatLevel = threatLevel,
            confidence = riskScore.coerceIn(0f, 1f),
            reason = buildReason(threatLevel, details),
            details = details,
            suggestedAction = getAdvice(threatLevel, details),
            requiresUserAttention = threatLevel.value >= ThreatLevel.SUSPICIOUS.value
        )
    }

    private fun scoreToThreatLevel(score: Float): ThreatLevel = when {
        score >= 0.7f -> ThreatLevel.CRITICAL
        score >= 0.5f -> ThreatLevel.MALICIOUS
        score >= 0.3f -> ThreatLevel.SUSPICIOUS
        else -> ThreatLevel.SAFE
    }

    private fun buildReason(level: ThreatLevel, details: Map<String, String>): String {
        return when {
            details.containsKey("unknown_sender_financial") -> "Financial request from an unknown sender."
            details.containsKey("otp_request") -> "Potential attempt to steal your verification code (OTP)."
            details.containsKey("insecure_payment_url") -> "Insecure payment link detected."
            level == ThreatLevel.SAFE -> "Payment environment looks secure."
            else -> "Review this transaction carefully."
        }
    }

    private fun getAdvice(level: ThreatLevel, details: Map<String, String>): String? {
        if (level == ThreatLevel.SAFE) return null
        return when {
            details.containsKey("otp_request") -> "NEVER share your OTP or verification codes with anyone, including bank staff."
            details.containsKey("unknown_sender_financial") -> "Verify the identity of the person requesting money through a different channel."
            else -> "Ensure you are using an official app and a secure connection before proceeding."
        }
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No payment activity detected"
    )
}
