package com.aegis.ai.safety

import com.aegis.ai.GeminiAIManager
import java.util.regex.Pattern

class SafetyClassifier(
    private val localModel: SafetyModel = RuleBasedSafetyModel()
) {
    private var geminiManager: GeminiAIManager? = null

    // Regex to detect links or phone numbers (High-risk indicators)
    private val URL_PATTERN = Pattern.compile(
        "https?://\\S+\\.\\S+|www\\.\\S+\\.\\S+|\\b\\d{4,5}[- ]?\\d{3,4}[- ]?\\d{3,4}\\b"
    )

    fun setGeminiManager(manager: GeminiAIManager) {
        this.geminiManager = manager
    }
    
    enum class SafetyCategory {
        SAFE,
        SCAM_FRAUD,
        PHISHING,
        MALICIOUS_CODE,
        HARASSMENT,
        HATE_SPEECH,
        MISINFORMATION
    }
    
    data class SafetyResult(
        val category: SafetyCategory,
        val confidence: Float
    ) {
        val isUnsafe: Boolean
            get() = category != SafetyCategory.SAFE
    }
    
    /**
     * Executes deep hybrid analysis: uses high-speed local keyword detection first.
     * STRICT QUOTA: Gemini is ONLY called if contextualSource is "CHATBOT".
     */
    suspend fun classify(text: String, contextualSource: String = "Notification"): SafetyResult {
        val textClean = text.trim()
        
        // 🛑 GATE 1: Ignore structural background noise and brief texts
        if (textClean.isEmpty() || textClean.length < 10) {
            return SafetyResult(SafetyCategory.SAFE, 1.0f)
        }

        // 🛑 GATE 2: Look for high-risk anomalies locally
        val hasUrlOrPhone = URL_PATTERN.matcher(textClean).find()
        val localResult = localModel.analyze(textClean)

        // If local rules say it's clean AND there are no sketchy links, exit immediately.
        if (!localResult.isUnsafe && !hasUrlOrPhone) {
            return SafetyResult(SafetyCategory.SAFE, 1.0f)
        }

        // 🛑 GATE 3: STRICT CLOUD GATING
        // Gemini is ONLY allowed for the Chatbot or if explicitly requested via a trigger.
        val isChatbot = contextualSource == "CHATBOT" || contextualSource == "DeepAudit"
        
        if (!isChatbot) {
            // Background scan: Return local result only to protect quota
            return localResult
        }

        val activeGemini = geminiManager
        if (activeGemini == null) return localResult

        // 🚀 ONLY CHATBOT TRAFFIC HITS GEMINI HERE
        return try {
            val aiAnalysis = activeGemini.generateConsolidatedResponse(
                prompt = textClean, 
                contextStr = "Source tracking type: $contextualSource"
            )
            
            if (aiAnalysis.isThreat) {
                val targetCategory = when {
                    localResult.category == SafetyCategory.HARASSMENT -> SafetyCategory.HARASSMENT
                    aiAnalysis.guidance.contains("phish", ignoreCase = true) -> SafetyCategory.PHISHING
                    else -> SafetyCategory.SCAM_FRAUD
                }
                SafetyResult(targetCategory, aiAnalysis.confidence)
            } else {
                SafetyResult(SafetyCategory.SAFE, 1.0f)
            }
        } catch (e: Exception) {
            localResult
        }
    }
}

interface SafetyModel {
    fun analyze(text: String): SafetyClassifier.SafetyResult
}

class RuleBasedSafetyModel : SafetyModel {
    
    private val scamPatterns = listOf(
        "urgent action required", "verify your account", "suspended account",
        "click here immediately", "act now", "limited time offer", "winner",
        "congratulations", "you have been selected", "claim your prize",
        "send money", "wire transfer", "bitcoin", "cryptocurrency",
        "inheritance", "lottery", "investment opportunity", "guaranteed returns",
        "secret investment", "insider trading", "pump and dump",
        "momo", "transfer successful", "transaction successful", "cash out",
        "sent you money", "return the money", "wrong number", "payout",
        "accidental transfer", "overdue payment", "debt collection",
        "one-time password", "otp verification", "account recovery code",
        "suspicious activity detected", "security alert: your account",
        "unauthorized transaction", "confirm your bank details",
        "accídental", "urgént", "paymėnt", "vėrify", "c1ick", "m0ney" // Common obfuscations
    )
    
    private val phishingPatterns = listOf(
        "verify your identity", "confirm your password", "update your information",
        "security alert", "unusual activity", "suspicious login",
        "click to verify", "confirm your email", "account verification",
        "billing information", "payment method", "credit card",
        "social security", "bank account", "routing number"
    )
    
    private val maliciousCodePatterns = listOf(
        "download this file", "install this app", "enable unknown sources",
        "disable antivirus", "turn off security", "bypass firewall",
        "execute this script", "run this command", "terminal",
        "root access", "jailbreak", "exploit", "vulnerability",
        "payload", "backdoor", "trojan", "malware", "ransomware"
    )
    
    private val harassmentPatterns = listOf(
        "stupid", "idiot", "loser", "worthless", "pathetic",
        "kill you", "kill yourself", "go die", "nobody likes you",
        "ugly", "disgusting", "repulsive", "freak", "cut you", "beat you",
        "your house", "your family", "know where you live", "murder"
    )
    

    
    private val misinformationPatterns = listOf(
        "fake news", "alternative facts", "conspiracy theory",
        "cover up", "they don't want you to know", "truth they hide",
        "mainstream media lies", "deep state", "secret cabal"
    )
    
    override fun analyze(text: String): SafetyClassifier.SafetyResult {
        val textLower = text.lowercase()
        
        // Count matches for each category
        val scamScore = scamPatterns.count { textLower.contains(it) }
        val phishingScore = phishingPatterns.count { textLower.contains(it) }
        val maliciousScore = maliciousCodePatterns.count { textLower.contains(it) }
        val harassmentScore = harassmentPatterns.count { textLower.contains(it) }
        val misinformationScore = misinformationPatterns.count { textLower.contains(it) }
        
        // Determine category with highest score
        val scores = mapOf(
            SafetyClassifier.SafetyCategory.SCAM_FRAUD to scamScore,
            SafetyClassifier.SafetyCategory.PHISHING to phishingScore,
            SafetyClassifier.SafetyCategory.MALICIOUS_CODE to maliciousScore,
            SafetyClassifier.SafetyCategory.HARASSMENT to harassmentScore,
            SafetyClassifier.SafetyCategory.MISINFORMATION to misinformationScore
        )
        
        val maxScore = scores.values.maxOrNull() ?: 0
        val maxCategory = scores.maxByOrNull { it.value }?.key
        
        // Calculate confidence based on score and text length
        val confidence = when {
            maxScore == 0 -> 1.0f // Safe
            maxScore >= 3 -> 0.95f
            maxScore >= 2 -> 0.85f
            maxScore >= 1 -> 0.70f
            else -> 0.5f
        }
        
        return if (maxScore > 0 && maxCategory != null) {
            SafetyClassifier.SafetyResult(maxCategory, confidence)
        } else {
            SafetyClassifier.SafetyResult(SafetyClassifier.SafetyCategory.SAFE, 1.0f)
        }
    }
}
