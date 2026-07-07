package com.aegis.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.ResponseCallback
import com.google.ai.edge.litertlm.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

class GemmaInferenceEngine(private val context: Context) : InferenceEngine, ReasoningEngine {
    
    private val TAG = "GemmaInferenceEngine"
    private val gemmaManager = GemmaModelManager(context)
    private var engine: Engine? = null
    private var session: Session? = null
    
    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    val isModelLoadedFlow: StateFlow<Boolean> get() = gemmaManager.isModelLoaded
    val isLoading: StateFlow<Boolean> get() = gemmaManager.isLoading
    val loadProgress: StateFlow<Float> get() = gemmaManager.loadProgress
    val errorMessage: StateFlow<String?> get() = gemmaManager.errorMessage

    /**
     * ReasoningEngine Implementation
     */
    override suspend fun generateResponse(prompt: String, context: String?): String {
        val formattedPrompt = formatPrompt(prompt, context)
        var response = ""
        
        try {
            ensureReady()
            val result = session?.generateContent(listOf(InputData.Text(formattedPrompt)))
            response = result ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            return "Error: ${e.localizedMessage}"
        }
        
        return response
    }

    override fun generateResponseStream(prompt: String, context: String?): Flow<String> = callbackFlow {
        val formattedPrompt = formatPrompt(prompt, context)
        try {
            if (!ensureReady()) {
                trySend("Error: Model engine not ready.")
                close()
                return@callbackFlow
            }

            session?.generateContentStream(
                listOf(InputData.Text(formattedPrompt)),
                object : ResponseCallback {
                    override fun onNext(response: String) {
                        trySend(response)
                    }

                    override fun onDone() {
                        close()
                    }

                    override fun onError(throwable: Throwable) {
                        close(throwable)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Stream generation failed", e)
            trySend("\n[Inference Error: ${e.localizedMessage}]")
            close(e)
        }
        awaitClose { 
            // In some versions we might want to cancel the session processing here
        }
    }.flowOn(Dispatchers.Default)

    private fun formatPrompt(prompt: String, context: String?): String {
        val systemContext = if (!context.isNullOrEmpty()) "Context: $context\n\n" else ""
        return "<start_of_turn>user\n${systemContext}$prompt<end_of_turn>\n<start_of_turn>model\n"
    }

    override suspend fun isModelLoaded(): Boolean = engine != null && gemmaManager.isModelLoaded.value

    override suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isModelLoaded()) return@withContext true
        
        val modelFile = gemmaManager.getModelFile()
        if (!modelFile.exists() || modelFile.length() == 0L) {
            Log.e(TAG, "Model file not found. Please install it first.")
            return@withContext false
        }

        _isInitializing.value = true
        gemmaManager.setLoading(true)
        
        try {
            val config = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.GPU(),
                maxNumTokens = 1024
            )
            
            val newEngine = Engine(config)
            newEngine.initialize()
            engine = newEngine
            session = newEngine.createSession()
            
            gemmaManager.setModelLoaded(true)
            Log.i(TAG, "Gemma 3N engine initialized successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Engine initialization failed", e)
            gemmaManager.setErrorMessage("Engine load error: ${e.localizedMessage}")
            false
        } finally {
            _isInitializing.value = false
            gemmaManager.setLoading(false)
        }
    }

    override fun getModelInfo(): ModelInfo {
        val gemmaInfo = gemmaManager.getModelInfo()
        return ModelInfo(
            name = gemmaInfo.name,
            type = "Gemma 3N LiteRT-LM",
            isLoaded = gemmaInfo.isLoaded,
            version = gemmaInfo.version,
            sizeBytes = gemmaInfo.size
        )
    }

    override suspend fun ensureReady(): Boolean {
        if (isModelLoaded()) return true
        return loadModel()
    }

    /**
     * InferenceEngine Implementation
     */
    override suspend fun classify(text: String, modelType: String): Float {
        val prompt = "On a scale from 0.0 to 1.0, where 1.0 is a definite scam and 0.0 is completely safe, " +
                     "rate this text for $modelType risk. Respond ONLY with the number.\nText: $text"
        val response = generateResponse(prompt)
        return response.trim().filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 0.5f
    }
    
    override suspend fun analyzeText(text: String, modelType: String): Map<String, Float> {
        val score = classify(text, modelType)
        return mapOf(
            "score" to score,
            "confidence" to 0.85f
        )
    }
    
    override suspend fun isModelLoaded(modelType: String): Boolean = isModelLoaded()
    
    override suspend fun loadModel(modelType: String): Boolean = loadModel()
    
    override fun getAvailableModels(): List<ModelInfo> = listOf(getModelInfo())

    /**
     * AEGIS Task-Specific AI Methods
     */

    suspend fun summarizeConversation(history: List<String>): String {
        val prompt = "Summarize the following conversation history and identify any suspicious behavior:\n" +
                     history.joinToString("\n")
        return generateResponse(prompt)
    }

    suspend fun analyzeConversation(history: List<String>, currentMessage: String): String {
        val prompt = "Analyze this conversation for risks:\nHistory: ${history.joinToString(" | ")}\nMessage: $currentMessage"
        return generateResponse(prompt)
    }

    suspend fun detectScam(text: String, metadata: Map<String, String>): Float {
        return classify(text, "scam")
    }

    suspend fun analyzeNotification(title: String, message: String, appName: String): String {
        val prompt = "Is this suspicious? App: $appName, Title: $title, Body: $message"
        return generateResponse(prompt)
    }

    fun getLoadProgress(): Float = gemmaManager.loadProgress.value
    
    fun getLoadError(): String? = gemmaManager.errorMessage.value
    
    fun installModel(): Flow<DownloadStatus> = gemmaManager.installModel()

    fun unloadModel() {
        session?.close()
        session = null
        engine?.close()
        engine = null
        gemmaManager.unloadModel()
    }

    fun close() {
        unloadModel()
    }
}
