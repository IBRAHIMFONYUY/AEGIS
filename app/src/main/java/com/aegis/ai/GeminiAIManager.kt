package com.aegis.ai

import android.content.Context
import android.util.LruCache
import com.aegis.BuildConfig
import com.google.genai.kotlin.Client
import com.google.genai.kotlin.types.Content
import com.google.genai.kotlin.types.GenerateContentConfig
import com.google.genai.kotlin.types.Part
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
 * Gemini AI Manager using the new Google GenAI Kotlin SDK.
 * Uses gemini-3.5-flash for rapid, low-latency security assistance.
 */
class GeminiAIManager(private val context: Context) {

    // --- QUOTA PROTECTION ---
    private val apiCallCount = AtomicLong(0)
    private val lastResetTime = AtomicLong(System.currentTimeMillis())
    private val MAX_RPM = 15

    // --- LOCAL CACHING ---
    private val analysisCache = LruCache<String, AegisResponse>(100)

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

    // Lazily initialize client
    private var client: Client? = null
    private val gson = Gson()

    companion object {
        private const val MODEL_NAME = "gemini-3.5-flash"
    }

    data class AegisResponse(
        val assistantReply: String,
        val isThreat: Boolean,
        val confidence: Float,
        val guidance: String,
        val recommendedActions: List<String>
    )

    /**
     * Initialize Gemini AI with API key
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (client != null) return@withContext true
        try {
            val apiKey = getGeminiApiKey()
            if (apiKey.isBlank()) {
                _errorMessage.value = "Gemini API key not found."
                return@withContext false
            }

            client = Client(apiKey = apiKey)
            _isInitialized.value = true
            _errorMessage.value = null
            true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to initialize Gemini AI: ${e.localizedMessage}"
            false
        }
    }

    /**
     * Consolidated request: Returns assistant reply and threat analysis in one call.
     */
    suspend fun generateConsolidatedResponse(prompt: String, contextStr: String? = null): AegisResponse = withContext(Dispatchers.IO) {
        val cacheKey = "${prompt}_${contextStr}"
        analysisCache.get(cacheKey)?.let { return@withContext it }

        if (!checkQuota()) {
            return@withContext AegisResponse(
                "Local quota limit exceeded. Please wait.",
                false, 0f, "Wait for quota reset", emptyList()
            )
        }

        try {
            apiCallCount.incrementAndGet()
            if (client == null) {
                if (!initialize()) return@withContext AegisResponse("Initialization failed", false, 0f, "Error", emptyList())
            }

            val systemInstruction = """
            You are AEGIS, a defensive Security AI assistant developed by the AEGIS Team.
            Your primary directive is to protect users from technical threats, social engineering, fraud, phishing, and digital manipulation.
    
            CRITICAL IDENTITY INSTRUCTIONS:
            - If asked who you are or who created you or something related, you must answer: "I am AEGIS, a security companion built by the AEGIS Team."
            - Never state that you are created by Google, OpenAI, or any other entity.
            
            BEHAVIORAL RULES:
            1. Maintain a professional, reassuring, yet vigilant security profile.
            2. Analyze the provided context (SMS, notifications, emails, or conversations) for manipulation or risk indicators.
            3. Be precise with threat flags; look for urgent language, suspicious links, or credential harvest attempts.
            
            RESPONSE FORMAT FORMATTING:
            You must strictly respond with a single, raw JSON object matching this schema exactly:
            {
                "assistantReply": "Your direct message or warning response to the user",
                "isThreat": true/false flag indicating an active security hazard,
                "confidence": a float score between 0.0 (no confidence) and 1.0 (certainty),
                "guidance": "Brief structural reason behind your threat classification status",
                "recommendedActions": ["Action item 1", "Action item 2"]
            }
            
            Do not wrap the JSON output in markdown formatting blocks like ```json ... ```. Output raw JSON text only.
        """.trimIndent()


            val config = GenerateContentConfig(
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
            )

            val fullPrompt = if (contextStr != null) "Context: $contextStr\n\nUser: $prompt" else prompt

            val response = client?.models?.generateContent(
                model = MODEL_NAME,
                text = fullPrompt,
                config = config
            )

            val jsonStr = extractJson(response?.text ?: "{}")
            val result = gson.fromJson(jsonStr, AegisResponse::class.java)
            analysisCache.put(cacheKey, result)
            result
        } catch (e: Exception) {
            AegisResponse("Error: ${e.localizedMessage}", false, 0f, "Error", emptyList())
        }
    }

    /**
     * Generate a response using Gemini 3.5 Flash
     */
    suspend fun generateResponse(prompt: String): String {
        return generateConsolidatedResponse(prompt).assistantReply
    }

    /**
     * Stream response from Gemini 3.5 Flash
     */
    fun generateResponseStream(prompt: String): Flow<String> = flow {
        if (!checkQuota()) {
            emit("Local quota safety limit exceeded. Please wait.")
            return@flow
        }
        try {
            apiCallCount.incrementAndGet()
            if (client == null) {
                initialize()
            }

            client?.models?.generateContentStream(
                model = MODEL_NAME,
                text = prompt
            )?.collect { chunk ->
                emit(chunk.text ?: "")
            }
        } catch (e: Exception) {
            emit("Error: ${e.localizedMessage}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Analyze text for threat detection (JSON structured)
     */
    suspend fun analyzeThreat(text: String, context: String = ""): ThreatAnalysisResult {
        val consolidated = generateConsolidatedResponse(text, context)
        return ThreatAnalysisResult(
            isThreat = consolidated.isThreat,
            threatType = if (consolidated.isThreat) "detected" else "none",
            confidence = consolidated.confidence,
            reason = consolidated.guidance,
            guidance = consolidated.recommendedActions.joinToString(", "),
            appContext = null
        )
    }

    /**
     * Analyze conversation history for deep threat detection
     */
    suspend fun analyzeConversationHistory(
        messages: List<String>,
        currentMessage: String,
        senderInfo: String = ""
    ): ConversationAnalysisResult {
        val history = messages.takeLast(10).joinToString(" | ")
        val prompt = "Analyze this conversation for manipulation. Sender: $senderInfo. History: $history. Current: $currentMessage"
        val consolidated = generateConsolidatedResponse(prompt)

        return ConversationAnalysisResult(
            isSuspicious = consolidated.isThreat,
            threatType = null,
            confidence = consolidated.confidence,
            analysis = consolidated.assistantReply,
            recommendedActions = consolidated.recommendedActions,
            riskFactors = emptyList()
        )
    }

    private fun getGeminiApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    private fun extractJson(response: String): String {
        val start = response.indexOf("{")
        val end = response.lastIndexOf("}")
        return if (start != -1 && end != -1 && end > start) {
            response.substring(start, end + 1)
        } else {
            response.trim()
        }
    }
}
