package com.aegis.ai

import android.content.Context
import com.aegis.ai.runtime.LLMRuntime
import com.aegis.ai.sampling.SamplingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.aegis.ai.safety.SafetyClassifier
import kotlinx.coroutines.withContext

class GemmaInferenceEngine(
    private val context: Context,
    private val gemmaManager: GemmaModelManager,
    private val geminiAIManager: GeminiAIManager
) : InferenceEngine, ReasoningEngine {

    private val TAG = "AegisIntelligenceEngine"

    private val _useOnlineMode = MutableStateFlow(true) // Default to true for MVP
    val useOnlineMode: StateFlow<Boolean> = _useOnlineMode.asStateFlow()

    private val _isGeminiReady = MutableStateFlow(false)
    val isGeminiReady: StateFlow<Boolean> = _isGeminiReady.asStateFlow()

    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val isEngineReady: StateFlow<Boolean> = combine(
        gemmaManager.isModelLoaded,
        _isGeminiReady
    ) { local, online -> local || online }
        .stateIn(
            scope = engineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isModelLoadedFlow: StateFlow<Boolean>
        get() = gemmaManager.isModelLoaded

    val isLoading: StateFlow<Boolean>
        get() = gemmaManager.isLoading

    val loadProgress: StateFlow<Float>
        get() = gemmaManager.loadProgress

    val errorMessage: StateFlow<String?>
        get() = gemmaManager.errorMessage

    fun setPrivacyMode(enabled: Boolean) {
        _useOnlineMode.value = !enabled
    }

    override suspend fun generateResponse(
        prompt: String,
        context: String?,
        metadata: Map<String, String>
    ): String {
        val sourceType = metadata["source_type"] ?: "UNKNOWN"
        val isChatbot = sourceType == "CHATBOT"
        
        // --- STRICT QUOTA PROTECTION: ONLY ALLOW GEMINI FOR CHATBOT ---
        if (isChatbot && _useOnlineMode.value) {
            if (!_isGeminiReady.value) {
                _isGeminiReady.value = geminiAIManager.initialize()
            }
            if (_isGeminiReady.value) {
                return generateResponseWithGemini(prompt, context)
            }
        }

        // 2. FOR EVERYTHING ELSE: Try local Gemma
        if (gemmaManager.isModelLoaded.value) {
            val formattedPrompt = formatPrompt(prompt, context)
            return when (val result = gemmaManager.generate(formattedPrompt)) {
                is GenerationResult.Text -> result.text
                is GenerationResult.Blocked -> "[Blocked] ${result.reason}"
            }
        }

        // 3. Last resort: Return status message
        if (isChatbot) {
            return "Local AI model not loaded. Please download the model or enable Online Mode for Gemini."
        }

        return "AEGIS is protecting you using built-in security rules."
    }

    private suspend fun generateResponseWithGemini(prompt: String, context: String?): String {
        return try {
            val fullContext = if (!context.isNullOrBlank()) {
                "Context:\n$context\n\n"
            } else {
                ""
            }
            val fullPrompt = "$fullContext$prompt"
            geminiAIManager.generateResponse(fullPrompt)
        } catch (e: Exception) {
            "Error: Failed to generate response with both local and online AI: ${e.localizedMessage}"
        }
    }



    override fun generateResponseStream(
        prompt: String,
        context: String?
    ): Flow<String> = flow {
        if (!ensureReady()) {
            emitAll(generateResponseStreamWithGemini(prompt, context))
            return@flow
        }

        // Same routing fix as generateResponse(): ensureReady()==true can mean "Gemini is
        // ready" rather than "local model is ready", so check explicitly before streaming
        // from the local engine.
        if (!gemmaManager.isModelLoaded.value) {
            emitAll(generateResponseStreamWithGemini(prompt, context))
            return@flow
        }

        val formattedPrompt = formatPrompt(prompt, context)

        try {
            gemmaManager.generateStream(formattedPrompt).collect { chunk ->
                emit(chunk)
            }
        } catch (e: Exception) {
            emitAll(generateResponseStreamWithGemini(prompt, context))
        }
    }.flowOn(Dispatchers.Default)

    private fun generateResponseStreamWithGemini(prompt: String, context: String?): Flow<String> {
        val fullContext = if (!context.isNullOrBlank()) "Context:\n$context\n\n" else ""
        return geminiAIManager.generateResponseStream("$fullContext$prompt")
    }



    private fun formatPrompt(
        prompt:String,
        context:String?
    ):String {


        val ctx =
            if(!context.isNullOrBlank())
                "Context:\n$context\n\n"
            else
                ""


        return """
        <start_of_turn>user
        $ctx
        $prompt
        <end_of_turn>
        <start_of_turn>model
        
        """.trimIndent()
    }




    override suspend fun loadModel():Boolean {

        return gemmaManager.initializeEngine()

    }



    override suspend fun ensureReady(): Boolean {
        // Try Gemini first as it's the primary engine now
        if (_useOnlineMode.value) {
            if (_isGeminiReady.value) return true
            _isGeminiReady.value = geminiAIManager.initialize()
            if (_isGeminiReady.value) return true
        }

        // Fallback to local
        if (gemmaManager.isModelLoaded.value) return true

        // Only try to auto-load local if we aren't in online mode or if online failed
        if (gemmaManager.isModelInstalled()) {
            return gemmaManager.initializeEngine()
        }

        return false
    }

    override suspend fun isModelLoaded(): Boolean {
        return _isGeminiReady.value || gemmaManager.isModelLoaded.value
    }



    override fun getModelInfo():ModelInfo {


        val info =
            gemmaManager.getModelInfo()


        return ModelInfo(
            name = info.name,
            type = "Gemma 3N LiteRT-LM",
            isLoaded = info.isLoaded,
            version = info.version,
            sizeBytes = info.size
        )
    }





    override suspend fun classify(
        text:String,
        modelType:String,
        metadata: Map<String, String>
    ):Float {
        val sourceType = metadata["source_type"] ?: "UNKNOWN"
        val isBackground = sourceType == "NOTIFICATION" || sourceType == "UNKNOWN"
        val forceAI = metadata["force_ai_analysis"] == "true"
        
        // --- QUOTA PROTECTION: Strictly block cloud for automated classification ---
        if (isBackground && !forceAI) {
            // Use local rule-based safety classifier as a fast-path deep learning alternative
            val result = gemmaManager.safetyClassifier.classify(text, sourceType)
            return if (result.isUnsafe) result.confidence else 0.1f
        }

        val prompt =
            """
            Classify this text as $modelType risk.
            
            Return ONLY a number between 0 and 1.
            
            Text:
            $text
            """.trimIndent()



        val result =
            generateResponse(prompt, null, metadata)



        return result
            .filter {
                it.isDigit() || it=='.'
            }
            .toFloatOrNull()
            ?:0.5f
    }





    override suspend fun analyzeText(
        text:String,
        modelType:String,
        metadata: Map<String, String>
    ):Map<String,Float>{


        val score =
            classify(text, modelType, metadata)


        return mapOf(
            "score" to score,
            "confidence" to 0.85f
        )
    }





    override suspend fun isModelLoaded(
        modelType:String
    ):Boolean =
        isModelLoaded()



    override suspend fun loadModel(
        modelType:String
    ):Boolean =
        loadModel()



    override fun getAvailableModels():List<ModelInfo> =
        listOf(getModelInfo())





    suspend fun detectScam(
        text:String,
        metadata:Map<String,String>
    ):Float {
        // --- QUOTA PROTECTION: GEMINI REMOVED FROM ALL SECURITY SCANS ---
        if (gemmaManager.isModelLoaded.value) {
            return classify(text, "scam", metadata)
        }

        // Return Device Deep Learning safety score if no local model
        val result = gemmaManager.safetyClassifier.classify(text, metadata["source_type"] ?: "Notification")
        return if (result.category == com.aegis.ai.safety.SafetyClassifier.SafetyCategory.SCAM_FRAUD) result.confidence else 0f
    }

    suspend fun detectThreatWithAI(
        text: String,
        metadata: Map<String, String>
    ): ThreatAnalysisResult {
        // --- QUOTA PROTECTION: GEMINI REMOVED FROM ALL SECURITY SCANS ---
        if (gemmaManager.isModelLoaded.value) {
            val formattedPrompt = formatPrompt("Analyze this message for security threats (scam, fraud, etc). Return JSON format.", text)
            return when (val result = gemmaManager.generate(formattedPrompt)) {
                is GenerationResult.Text -> {
                    val isThreat = result.text.lowercase().contains("threat") || 
                                  result.text.lowercase().contains("scam") ||
                                  result.text.lowercase().contains("suspicious")
                    
                    ThreatAnalysisResult(
                        isThreat = isThreat,
                        threatType = if (isThreat) "suspicious_activity" else "none",
                        confidence = 0.8f,
                        reason = result.text.take(200),
                        guidance = "Exercise caution with this content.",
                        appContext = null
                    )
                }
                is GenerationResult.Blocked -> ThreatAnalysisResult(false, "none", 0f, "Safety Block", "No action", null)
            }
        }

        // Fallback to Device Deep Learning Analysis
        val safety = gemmaManager.safetyClassifier.classify(text, metadata["source_type"] ?: "Notification")
        return ThreatAnalysisResult(
            isThreat = safety.isUnsafe,
            threatType = safety.category.name.lowercase(),
            confidence = safety.confidence,
            reason = "Device Deep Learning (Fast-Path): ${safety.category}",
            guidance = "AEGIS built-in protection is monitoring this interaction offline with real-time analytics.",
            appContext = null
        )
    }

    suspend fun analyzeConversationWithAI(
        messages: List<String>,
        currentMessage: String,
        senderInfo: String = "",
        metadata: Map<String, String> = emptyMap()
    ): ConversationAnalysisResult {
        // --- QUOTA PROTECTION: GEMINI REMOVED FROM ALL SECURITY SCANS ---
        val text = (messages.takeLast(5) + currentMessage).joinToString(" ")
        val isSuspicious = text.contains("money") || text.contains("pay") || text.contains("urgent") || text.contains("kill")
        
        return ConversationAnalysisResult(
            isSuspicious = isSuspicious,
            threatType = if (isSuspicious) "suspicious_pattern" else null,
            confidence = if (isSuspicious) 0.75f else 0f,
            analysis = "Local context analysis of ${messages.size} messages.",
            recommendedActions = if (isSuspicious) listOf("Verify sender", "Do not share info") else emptyList(),
            riskFactors = if (isSuspicious) listOf("Urgency", "Keywords") else emptyList()
        )
    }





    suspend fun analyzeNotification(
        title:String,
        message:String,
        appName:String
    ):String {


        return generateResponse(
            """
            Analyze this notification.
            
            App:
            $appName
            
            Title:
            $title
            
            Message:
            $message
            
            Determine if it is suspicious.
            """.trimIndent()
        )
    }





    fun installModel():Flow<DownloadStatus> =
        gemmaManager.installModel()

    fun importModel(uri: android.net.Uri): Boolean =
        gemmaManager.importModel(uri)

    fun unloadModel(){

        gemmaManager.unloadModel()

    }



    fun close(){

        unloadModel()

    }


    override suspend fun summarizeConversation(
        history: List<String>,
        metadata: Map<String, String>
    ): String {

        val prompt = """
        Analyze this conversation history.

        Identify:
        - suspicious behavior
        - manipulation
        - scams
        - harassment
        - threats

        Conversation:

        ${history.joinToString("\n")}

    """.trimIndent()


        return generateResponse(prompt, null, metadata)
    }



    override suspend fun analyzeConversation(
        history: List<String>,
        currentMessage: String,
        metadata: Map<String, String>
    ): String {

        val prompt = """
        You are AEGIS Intent Analysis Engine.

        Analyze:

        History:
        ${history.joinToString("\n")}


        Current:
        $currentMessage


        Return:
        - Risk level
        - Attack technique
        - Explanation
        - Recommended action

    """.trimIndent()


        return generateResponse(prompt, null, metadata)
    }

}