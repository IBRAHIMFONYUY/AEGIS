package com.aegis.ai.safety

class SafetyClassifier(private val model: SafetyModel) {
    
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
    )
    
    fun classify(text: String): SafetyResult {
        return model.analyze(text)
    }
}

interface SafetyModel {
    fun analyze(text: String): SafetyClassifier.SafetyResult
}

class RuleBasedSafetyModel : SafetyModel {
    override fun analyze(text: String): SafetyClassifier.SafetyResult {
        val textLower = text.lowercase()
        return when {
            textLower.contains("scam") || textLower.contains("money") -> 
                SafetyClassifier.SafetyResult(SafetyClassifier.SafetyCategory.SCAM_FRAUD, 0.8f)
            else -> SafetyClassifier.SafetyResult(SafetyClassifier.SafetyCategory.SAFE, 1.0f)
        }
    }
}
