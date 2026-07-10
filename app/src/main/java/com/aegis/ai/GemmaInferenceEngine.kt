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
import kotlinx.coroutines.withContext

class GemmaInferenceEngine(
    private val context: Context
) : InferenceEngine, ReasoningEngine {

    private val TAG = "AegisIntelligenceEngine"

    private val gemmaManager = GemmaModelManager(context)
    private val geminiAIManager = GeminiAIManager(context)

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
        val isNotification = sourceType == "NOTIFICATION"
        
        // Force Cloud AI if metadata indicates a high-priority user request (like Chatbot)
        val forceAI = metadata["force_ai_analysis"] == "true" || isChatbot
        
        // --- CRITICAL QUOTA FIX: NEVER hit Cloud AI for background notifications ---
        val canUseCloud = (_useOnlineMode.value && !isNotification) || forceAI

        // 1. Try Gemini first if explicitly allowed/forced
        if (canUseCloud) {
            if (!_isGeminiReady.value) {
                _isGeminiReady.value = geminiAIManager.initialize()
            }
            if (_isGeminiReady.value) {
                return generateResponseWithGemini(prompt, context)
            }
        }

        // 2. Fallback/Privacy Mode: Try local Gemma
        if (gemmaManager.isModelLoaded.value) {
            val formattedPrompt = formatPrompt(prompt, context)
            return when (val result = gemmaManager.generate(formattedPrompt)) {
                is GenerationResult.Text -> result.text
                is GenerationResult.Blocked -> "[Blocked] ${result.reason}"
            }
        }

        // 3. Last resort: Return status message
        if (isChatbot) {
            return "Gemini is currently unavailable. Please check your internet connection or enable Privacy Mode after downloading the local model."
        }

        return "Local AI model not loaded. AEGIS is protecting you using real-time security rules."
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
            return 0.1f // Default safe score
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
        // 1. Try local model first
        if (gemmaManager.isModelLoaded.value) {
            return classify(text, "scam", metadata)
        }

        // 2. Only fallback to Gemini if NOT a background notification
        val sourceType = metadata["source_type"] ?: "UNKNOWN"
        val isNotification = sourceType == "NOTIFICATION"
        val forceAI = metadata["force_ai_analysis"] == "true"
        
        if ((_useOnlineMode.value && !isNotification) || forceAI) {
            val context = metadata.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            val result = geminiAIManager.analyzeThreat(text, context)
            return if (result.isThreat && result.threatType == "scam") {
                result.confidence
            } else {
                0f
            }
        }

        // If cloud is restricted, return 0 (let local rules handle it)
        return 0f
    }

    suspend fun detectThreatWithAI(
        text: String,
        metadata: Map<String, String>
    ): ThreatAnalysisResult {
        // Only use Gemini if not a background notification
        val sourceType = metadata["source_type"] ?: "UNKNOWN"
        val isNotification = sourceType == "NOTIFICATION"
        val forceAI = metadata["force_ai_analysis"] == "true"

        if ((_useOnlineMode.value && !isNotification) || forceAI) {
            val context = metadata.entries.joinToString("\n") { "${it.key}: ${it.value}" }
            return geminiAIManager.analyzeThreat(text, context)
        }
        
        return ThreatAnalysisResult(false, "none", 0f, "Cloud restricted for background tasks", "No action", null)
    }

    suspend fun analyzeConversationWithAI(
        messages: List<String>,
        currentMessage: String,
        senderInfo: String = "",
        metadata: Map<String, String> = emptyMap()
    ): ConversationAnalysisResult {
        val sourceType = metadata["source_type"] ?: "UNKNOWN"
        val isNotification = sourceType == "NOTIFICATION"
        val forceAI = metadata["force_ai_analysis"] == "true"

        if ((_useOnlineMode.value && !isNotification) || forceAI) {
            return geminiAIManager.analyzeConversationHistory(messages, currentMessage, senderInfo)
        }

        return ConversationAnalysisResult(false, null, 0f, "Cloud restricted for background tasks", emptyList(), emptyList())
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