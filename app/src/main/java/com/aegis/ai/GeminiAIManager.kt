package com.aegis.ai

import android.content.Context
import com.aegis.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Gemini AI Manager for online AI fallback when local model is not available.
 * Uses Google Gemini API for cloud-based AI inference.
 */
class GeminiAIManager(private val context: Context) {
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: Flow<Boolean> = _isInitialized.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: Flow<String?> = _errorMessage.asStateFlow()
    
    private var generativeModel: GenerativeModel? = null
    
    companion object {
        private const val TAG = "GeminiAIManager"
        private const val MODEL_NAME = "gemini-1.5-flash"
    }
    
    /**
     * Initialize Gemini AI with API key from BuildConfig
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiKey = getGeminiApiKey()
            if (apiKey.isBlank()) {
                _errorMessage.value = "Gemini API key not found. Please add GEMINI_API_KEY to local.properties"
                return@withContext false
            }
            
            generativeModel = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey
            )
            
            _isInitialized.value = true
            _errorMessage.value = null
            true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to initialize Gemini AI: ${e.localizedMessage}"
            false
        }
    }
    
    /**
     * Generate a response using Gemini API
     */
    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: initializeAndGetModel()
            
            val response = model?.generateContent(
                content {
                    text(prompt)
                }
            )
            
            response?.text ?: "Failed to generate response"
        } catch (e: Exception) {
            "Error generating response: ${e.localizedMessage}"
        }
    }
    
    /**
     * Stream response from Gemini API
     */
    fun generateResponseStream(prompt: String): Flow<String> = flow {
        try {
            val model = generativeModel ?: initializeAndGetModel()
            
            model?.generateContentStream(
                content {
                    text(prompt)
                }
            )?.collect { chunk ->
                emit(chunk.text ?: "")
            }
        } catch (e: Exception) {
            emit("Error: ${e.localizedMessage}")
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Analyze text for threat detection
     */
    suspend fun analyzeThreat(text: String, context: String = ""): ThreatAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: initializeAndGetModel()
            
            val prompt = buildThreatAnalysisPrompt(text, context)
            
            val response = model?.generateContent(
                content {
                    text(prompt)
                }
            )
            
            parseThreatAnalysis(response?.text ?: "")
        } catch (e: Exception) {
            ThreatAnalysisResult(
                isThreat = false,
                threatType = "unknown",
                confidence = 0f,
                reason = "Analysis failed: ${e.localizedMessage}",
                guidance = "Please try again later",
                appContext = null
            )
        }
    }
    
    /**
     * Analyze conversation history for deep threat detection
     */
    suspend fun analyzeConversationHistory(
        messages: List<String>,
        currentMessage: String,
        senderInfo: String = ""
    ): ConversationAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val model = generativeModel ?: initializeAndGetModel()
            
            val prompt = buildConversationAnalysisPrompt(messages, currentMessage, senderInfo)
            
            val response = model?.generateContent(
                content {
                    text(prompt)
                }
            )
            
            parseConversationAnalysis(response?.text ?: "")
        } catch (e: Exception) {
            ConversationAnalysisResult(
                isSuspicious = false,
                threatType = null,
                confidence = 0f,
                analysis = "Analysis failed: ${e.localizedMessage}",
                recommendedActions = emptyList(),
                riskFactors = emptyList()
            )
        }
    }
    
    private suspend fun initializeAndGetModel(): GenerativeModel? {
        return if (initialize()) {
            generativeModel
        } else {
            null
        }
    }
    
    private fun getGeminiApiKey(): String {
        // Try to get from BuildConfig first
        try {
            val apiKey = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
            if (!apiKey.isNullOrBlank()) return apiKey
        } catch (e: Exception) {
            // Field doesn't exist, try other methods
        }
        
        // Try to get from system properties or environment
        return System.getProperty("GEMINI_API_KEY") ?: ""
    }
    
    private fun buildThreatAnalysisPrompt(text: String, context: String): String {
        return """
            You are AEGIS, an advanced AI security guardian. Analyze the following text for potential threats.
            
            Context: $context
            
            Text to analyze:
            $text
            
            Provide your analysis in the following JSON format:
            {
                "isThreat": true/false,
                "threatType": "scam/phishing/malware/harassment/impersonation/fake_news/none",
                "confidence": 0.0 to 1.0,
                "reason": "Detailed explanation of why this is or isn't a threat",
                "guidance": "Specific actions the user should take",
                "appContext": "If applicable, mention which app or platform this is from"
            }
            
            Be thorough but concise. Focus on detecting:
            - Scams and fraud
            - Phishing attempts
            - Malicious links
            - Harassment or bullying
            - Impersonation
            - Misinformation
        """.trimIndent()
    }
    
    private fun buildConversationAnalysisPrompt(
        messages: List<String>,
        currentMessage: String,
        senderInfo: String
    ): String {
        val last20Messages = messages.takeLast(20)
        
        return """
            You are AEGIS, an advanced AI security guardian. Analyze this conversation for potential threats.
            
            Sender Info: $senderInfo
            
            Last 20 messages in the conversation:
            ${last20Messages.joinToString("\n") { "[${last20Messages.indexOf(it) + 1}] $it" }}
            
            Current message to analyze:
            $currentMessage
            
            Provide your analysis in the following JSON format:
            {
                "isSuspicious": true/false,
                "threatType": "scam/phishing/harassment/impersonation/social_engineering/romance_scam/none",
                "confidence": 0.0 to 1.0,
                "analysis": "Detailed analysis of the conversation patterns and potential threats",
                "recommendedActions": ["action1", "action2", "action3"],
                "riskFactors": ["factor1", "factor2", "factor3"]
            }
            
            Look for:
            - Pattern of manipulation
            - Urgency or pressure tactics
            - Requests for money or personal information
            - Inconsistent stories
            - Attempts to move conversation to other platforms
            - Romance scam patterns
            - Investment or cryptocurrency scams
        """.trimIndent()
    }
    
    private fun parseThreatAnalysis(response: String): ThreatAnalysisResult {
        return try {
            // Simple JSON parsing - in production, use proper JSON parser
            val isThreat = response.contains("\"isThreat\":true")
            val threatType = extractJsonValue(response, "threatType") ?: "unknown"
            val confidence = extractJsonValue(response, "confidence")?.toFloatOrNull() ?: 0f
            val reason = extractJsonValue(response, "reason") ?: "No detailed reason provided"
            val guidance = extractJsonValue(response, "guidance") ?: "No specific guidance available"
            val appContext = extractJsonValue(response, "appContext")
            
            ThreatAnalysisResult(
                isThreat = isThreat,
                threatType = threatType,
                confidence = confidence,
                reason = reason,
                guidance = guidance,
                appContext = appContext
            )
        } catch (e: Exception) {
            ThreatAnalysisResult(
                isThreat = false,
                threatType = "unknown",
                confidence = 0f,
                reason = "Failed to parse analysis",
                guidance = "Please try again",
                appContext = null
            )
        }
    }
    
    private fun parseConversationAnalysis(response: String): ConversationAnalysisResult {
        return try {
            val isSuspicious = response.contains("\"isSuspicious\":true")
            val threatType = extractJsonValue(response, "threatType")
            val confidence = extractJsonValue(response, "confidence")?.toFloatOrNull() ?: 0f
            val analysis = extractJsonValue(response, "analysis") ?: "No analysis available"
            val recommendedActions = extractJsonArray(response, "recommendedActions")
            val riskFactors = extractJsonArray(response, "riskFactors")
            
            ConversationAnalysisResult(
                isSuspicious = isSuspicious,
                threatType = threatType,
                confidence = confidence,
                analysis = analysis,
                recommendedActions = recommendedActions,
                riskFactors = riskFactors
            )
        } catch (e: Exception) {
            ConversationAnalysisResult(
                isSuspicious = false,
                threatType = null,
                confidence = 0f,
                analysis = "Failed to parse conversation analysis",
                recommendedActions = emptyList(),
                riskFactors = emptyList()
            )
        }
    }
    
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"?([^\"\\n,}]+)\"?".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)?.trim()
    }
    
    private fun extractJsonArray(json: String, key: String): List<String> {
        try {
            val pattern = "\"$key\"\\s*:\\s*\\[(.*?)\\]".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = pattern.find(json)
            val arrayContent = match?.groupValues?.get(1) ?: return emptyList()
            
            return arrayContent.split(",").map { it.trim().removeSurrounding("\"") }
        } catch (e: Exception) {
            return emptyList()
        }
    }
}

data class ThreatAnalysisResult(
    val isThreat: Boolean,
    val threatType: String,
    val confidence: Float,
    val reason: String,
    val guidance: String,
    val appContext: String?
)

data class ConversationAnalysisResult(
    val isSuspicious: Boolean,
    val threatType: String?,
    val confidence: Float,
    val analysis: String,
    val recommendedActions: List<String>,
    val riskFactors: List<String>
)
