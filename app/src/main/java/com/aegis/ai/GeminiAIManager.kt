package com.aegis.ai

import android.content.Context
import com.aegis.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

import java.util.concurrent.atomic.AtomicLong
import timber.log.Timber

/**
 * Gemini AI Manager for online AI fallback when local model is not available.
 * Uses Google Gemini API for cloud-based AI inference.
 */
class GeminiAIManager(private val context: Context) {
    
    // --- QUOTA PROTECTION ---
    private val apiCallCount = AtomicLong(0)
    private val lastResetTime = AtomicLong(System.currentTimeMillis())
    private val MAX_RPM = 15 // Increased to 15 (Gemini Free Tier limit)
    
    private fun checkQuota(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastResetTime.get() > 60000) {
            apiCallCount.set(0)
            lastResetTime.set(now)
            return true
        }
        return apiCallCount.get() < MAX_RPM
    }
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: Flow<Boolean> = _isInitialized.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: Flow<String?> = _errorMessage.asStateFlow()
    
    private var generativeModel: GenerativeModel? = null
    
    private val gson = Gson()
    
    companion object {
        private const val MODEL_NAME = "gemini-2.5-flash"
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
        if (!checkQuota()) {
            Timber.tag("GeminiQuota").w("Quota Exceeded (RPM Limit)")
            return@withContext "Cloud AI Quota Exceeded. Please try again in a minute or enable Privacy Mode."
        }
        try {
            apiCallCount.incrementAndGet()
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
        if (!checkQuota()) {
            Timber.tag("GeminiQuota").w("Quota Exceeded (RPM Limit)")
            return@withContext ThreatAnalysisResult(false, "none", 0f, "Quota Exceeded", "Try later", null)
        }
        try {
            apiCallCount.incrementAndGet()
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
        if (!checkQuota()) {
            Timber.tag("GeminiQuota").w("Quota Exceeded (RPM Limit)")
            return@withContext ConversationAnalysisResult(false, null, 0f, "Quota Exceeded", emptyList(), emptyList())
        }
        try {
            apiCallCount.incrementAndGet()
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
            // Use reflection to check if field exists, to avoid compilation error if it doesn't
            val buildConfigClass = BuildConfig::class.java
            val apiKeyField = buildConfigClass.getField("GEMINI_API_KEY")
            val apiKey = apiKeyField.get(null) as? String
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
            val jsonString = extractJson(response)
            gson.fromJson(jsonString, ThreatAnalysisResult::class.java)
        } catch (e: Exception) {
            ThreatAnalysisResult(
                isThreat = response.lowercase().contains("threat") || response.lowercase().contains("suspicious"),
                threatType = "unknown",
                confidence = 0.5f,
                reason = "Failed to parse JSON analysis: ${e.localizedMessage}. Raw response: $response",
                guidance = "Please exercise caution.",
                appContext = null
            )
        }
    }
    
    private fun parseConversationAnalysis(response: String): ConversationAnalysisResult {
        return try {
            val jsonString = extractJson(response)
            gson.fromJson(jsonString, ConversationAnalysisResult::class.java)
        } catch (e: Exception) {
            ConversationAnalysisResult(
                isSuspicious = response.lowercase().contains("suspicious") || response.lowercase().contains("scam"),
                threatType = null,
                confidence = 0.5f,
                analysis = "Failed to parse JSON analysis: ${e.localizedMessage}. Raw response: $response",
                recommendedActions = emptyList(),
                riskFactors = emptyList()
            )
        }
    }

    private fun extractJson(response: String): String {
        return if (response.contains("```json")) {
            response.substringAfter("```json").substringBefore("```").trim()
        } else if (response.contains("```")) {
            response.substringAfter("```").substringBefore("```").trim()
        } else {
            val start = response.indexOf("{")
            val end = response.lastIndexOf("}")
            if (start != -1 && end != -1 && end > start) {
                response.substring(start, end + 1)
            } else {
                response.trim()
            }
        }
    }
}
