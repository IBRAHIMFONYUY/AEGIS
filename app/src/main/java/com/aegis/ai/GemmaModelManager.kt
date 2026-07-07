package com.aegis.ai

import android.content.Context
import com.aegis.ai.cache.FlatKVCache
import com.aegis.ai.runtime.LLMRuntime
import com.aegis.ai.runtime.LLMRuntimeFactory
import com.aegis.ai.safety.SafetyClassifier
import com.aegis.ai.sampling.Sampler
import com.aegis.ai.sampling.SamplingParams
import com.aegis.ai.tokenizer.BPETokenizer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class GemmaModelManager(private val context: Context) {

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadProgress = MutableStateFlow(0f)
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var llmRuntime: LLMRuntime? = null
    private var interpreter: LiteRTInterpreter? = null

    companion object {
        private const val MODEL_FILE_NAME = "gemma-3n-q8.tflite"
        private const val MODEL_VERSION = "1.0.0"
        private const val MODEL_SIZE_BYTES = 1_500_000_000L // ~1.5GB
    }

    suspend fun loadModel(): Boolean {
        if (_isModelLoaded.value) return true

        _isLoading.value = true
        _errorMessage.value = null

        try {
            _loadProgress.value = 0.1f

            // Check if model exists, if not download it
            val modelFile = File(context.filesDir, MODEL_FILE_NAME)
            if (!modelFile.exists()) {
                downloadModel(modelFile)
            }

            _loadProgress.value = 0.5f

            // Load TFLite interpreter
            val options = LiteRTInterpreter.Options()
                .setNumThreads(4)
                .setUseNNAPI(true)

            interpreter = LiteRTInterpreter.createFromFile(modelFile.absolutePath, options)

            _loadProgress.value = 0.7f

            // Create tokenizer
            val tokenizer = BPETokenizer.createMinimal()

            // Create sampler
            val sampler = Sampler(tokenizer.vocabSize())

            // Create safety classifier
            val safetyModel = com.aegis.ai.safety.RuleBasedSafetyModel()
            val safetyClassifier = SafetyClassifier(safetyModel)

            // Create LLM runtime with real components
            val config = com.aegis.ai.runtime.LLMConfig(
                vocabSize = tokenizer.vocabSize(),
                maxSeqLen = 512,
                numLayers = 28,
                numHeads = 32,
                headDim = 128
            )

            llmRuntime = LLMRuntimeFactory.create(
                interpreter = interpreter!!,
                tokenizer = tokenizer,
                sampler = sampler,
                safetyClassifier = safetyClassifier,
                config = config
            )

            // Initialize runtime
            llmRuntime?.initialize()

            _loadProgress.value = 1.0f
            _isModelLoaded.value = true
            _isLoading.value = false

            return true
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load model: ${e.message}"
            _isLoading.value = false
            _isModelLoaded.value = false
            return false
        }
    }

    private suspend fun downloadModel(destinationFile: File) {
        _loadProgress.value = 0.2f

        // Check available storage
        val availableSpace = context.filesDir.freeSpace
        if (availableSpace < MODEL_SIZE_BYTES * 1.2) {
            throw InsufficientStorageException("Not enough storage space. Required: ${MODEL_SIZE_BYTES / (1024 * 1024)}MB")
        }

        _loadProgress.value = 0.25f

        // Real Download Implementation
        val url = "https://huggingface.co/google/gemma-3n-it-tflite/resolve/main/$MODEL_FILE_NAME"
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(url).build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("Failed to download model: $response")
                
                val body = response.body ?: throw java.io.IOException("Empty response body")
                val contentLength = body.contentLength()
                
                destinationFile.parentFile?.mkdirs()
                
                body.byteStream().use { input ->
                    destinationFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (contentLength > 0) {
                                _loadProgress.value = 0.25f + (totalBytesRead.toFloat() / contentLength * 0.4f)
                            }
                        }
                    }
                }
            }
        }
        
        _loadProgress.value = 0.65f
    }

    fun classifyText(text: String, task: String): Float {
        if (!_isModelLoaded.value || llmRuntime == null) {
            return 0f
        }

        return try {
            // Use safety classifier for classification
            val safetyResult = llmRuntime?.classifySafety(text)

            // Convert safety result to scam probability
            when (safetyResult?.category) {
                SafetyClassifier.SafetyCategory.SCAM_FRAUD -> safetyResult.confidence
                SafetyClassifier.SafetyCategory.PHISHING -> safetyResult.confidence * 0.9f
                SafetyClassifier.SafetyCategory.MALICIOUS_CODE -> safetyResult.confidence * 0.8f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    fun generateResponse(prompt: String, context: String = ""): String {
        if (!_isModelLoaded.value || llmRuntime == null) {
            return "Model not loaded"
        }

        return try {
            val fullPrompt = if (context.isNotEmpty()) {
                "$context\n\n$prompt"
            } else {
                prompt
            }

            val params = SamplingParams.balanced()
            val result = llmRuntime?.generate(fullPrompt, params)

            result?.text ?: "Generation failed"
        } catch (e: Exception) {
            "Error generating response: ${e.message}"
        }
    }

    fun getRuntimeStats(): com.aegis.ai.runtime.RuntimeStats? {
        return llmRuntime?.getStats()
    }

    fun unloadModel() {
        llmRuntime?.close()
        llmRuntime = null
        interpreter?.close()
        interpreter = null
        _isModelLoaded.value = false
    }

    fun getModelInfo(): GemmaModelInfo {
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        return GemmaModelInfo(
            name = "Gemma 3N",
            version = MODEL_VERSION,
            size = if (modelFile.exists()) modelFile.length() else 0L,
            isLoaded = _isModelLoaded.value,
            loadedAt = if (_isModelLoaded.value) System.currentTimeMillis() else null
        )
    }

    fun clearModelCache() {
        val modelFile = File(context.filesDir, MODEL_FILE_NAME)
        if (modelFile.exists()) {
            modelFile.delete()
        }
        unloadModel()
    }
}

data class GemmaModelInfo(
    val name: String,
    val version: String,
    val size: Long,
    val isLoaded: Boolean,
    val loadedAt: Long?
) {
    fun getSizeInMB(): Double = size / (1024.0 * 1024.0)
}

class InsufficientStorageException(message: String) : Exception(message)
