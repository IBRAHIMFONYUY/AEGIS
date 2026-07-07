package com.aegis.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class GemmaInferenceEngine(private val context: Context) : InferenceEngine, ReasoningEngine {
    
    private val gemmaManager = GemmaModelManager(context)
    
    val isModelLoadedFlow: StateFlow<Boolean> get() = gemmaManager.isModelLoaded
    val isLoading: StateFlow<Boolean> get() = gemmaManager.isLoading
    val loadProgress: StateFlow<Float> get() = gemmaManager.loadProgress
    val errorMessage: StateFlow<String?> get() = gemmaManager.errorMessage

    /**
     * ReasoningEngine Implementation
     */
    override suspend fun generateResponse(prompt: String, context: String?): String {
        return withContext(Dispatchers.Default) {
            if (!gemmaManager.isModelLoaded.value) {
                gemmaManager.loadModel()
            }
            gemmaManager.generateResponse(prompt, context ?: "")
        }
    }

    override fun generateResponseStream(prompt: String, context: String?): Flow<String> {
        return flow {
            emit(generateResponse(prompt, context))
        }
    }

    override suspend fun isModelLoaded(): Boolean = gemmaManager.isModelLoaded.value

    override suspend fun loadModel(): Boolean = gemmaManager.loadModel()

    override fun getModelInfo(): ModelInfo {
        val gemmaInfo = gemmaManager.getModelInfo()
        return ModelInfo(
            name = gemmaInfo.name,
            type = "Gemma 3N",
            isLoaded = gemmaInfo.isLoaded,
            version = gemmaInfo.version,
            sizeBytes = gemmaInfo.size
        )
    }

    /**
     * InferenceEngine Implementation
     */
    override suspend fun classify(text: String, modelType: String): Float {
        return withContext(Dispatchers.Default) {
            if (!gemmaManager.isModelLoaded.value) {
                gemmaManager.loadModel()
            }
            
            when (modelType) {
                "scam_detection" -> gemmaManager.classifyText(text, "scam_detection")
                "intent_analysis" -> gemmaManager.classifyText(text, "intent_analysis")
                "behavioral_analysis" -> gemmaManager.classifyText(text, "behavioral_analysis")
                "deepfake_detection" -> gemmaManager.classifyText(text, "deepfake_detection")
                else -> 0f
            }
        }
    }
    
    override suspend fun analyzeText(text: String, modelType: String): Map<String, Float> {
        return withContext(Dispatchers.Default) {
            if (!gemmaManager.isModelLoaded.value) {
                gemmaManager.loadModel()
            }
            
            val scamScore = gemmaManager.classifyText(text, "scam_detection")
            val intentScore = gemmaManager.classifyText(text, "intent_analysis")
            val behavioralScore = gemmaManager.classifyText(text, "behavioral_analysis")
            
            mapOf(
                "scam" to scamScore,
                "intent" to intentScore,
                "behavioral" to behavioralScore,
                "overall" to (scamScore * 0.4f + intentScore * 0.3f + behavioralScore * 0.3f)
            )
        }
    }
    
    override suspend fun isModelLoaded(modelType: String): Boolean = isModelLoaded()
    
    override suspend fun loadModel(modelType: String): Boolean = loadModel()
    
    override fun getAvailableModels(): List<ModelInfo> = listOf(getModelInfo())

    /**
     * AEGIS Task-Specific AI Methods
     */

    suspend fun analyzeConversation(history: List<String>, currentMessage: String): String {
        val prompt = "Analyze the following conversation for relationship dynamics and potential risks.\n" +
                     "History: ${history.joinToString(" | ")}\n" +
                     "Current Message: $currentMessage\n" +
                     "Provide a concise relationship confidence assessment and list manipulation techniques if any."
        return generateResponse(prompt)
    }

    suspend fun detectScam(text: String, metadata: Map<String, String>): Float {
        val prompt = "Evaluate if this message is a scam or phishing attempt.\n" +
                     "Content: $text\n" +
                     "Metadata: ${metadata.entries.joinToString { "${it.key}=${it.value}" }}\n" +
                     "Respond with a single numerical risk score between 0 and 1."
        val response = generateResponse(prompt)
        return response.trim().toFloatOrNull() ?: 0.5f
    }

    suspend fun analyzeNotification(title: String, message: String, appName: String): String {
        val prompt = "Analyze this notification from $appName.\n" +
                     "Title: $title\n" +
                     "Body: $message\n" +
                     "Is this notification suspicious? Explain why in one short sentence."
        return generateResponse(prompt)
    }

    suspend fun assessPrivacyRisk(appName: String, permission: String, rationale: String): String {
        val prompt = "App '$appName' is requesting '$permission' permission.\n" +
                     "App Rationale (if any): $rationale\n" +
                     "Is this request standard or invasive for this type of app? Provide a recommendation."
        return generateResponse(prompt)
    }

    suspend fun summarizeConversation(history: List<String>): String {
        val prompt = "Summarize the key security-related behavioral patterns in this conversation history:\n" +
                     history.joinToString("\n")
        return generateResponse(prompt)
    }

    fun getLoadProgress(): Float = gemmaManager.loadProgress.value
    
    fun getLoadError(): String? = gemmaManager.errorMessage.value
    
    fun unloadModel() = gemmaManager.unloadModel()
}
