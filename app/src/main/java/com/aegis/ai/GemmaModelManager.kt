package com.aegis.ai

import android.content.Context
import com.aegis.BuildConfig
import com.aegis.ai.safety.SafetyClassifier
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed interface DownloadStatus {
    data class Progress(val percentage: Int) : DownloadStatus
    data class Success(val file: File) : DownloadStatus
    data class Error(val message: String) : DownloadStatus
}

/**
 * Result of a generation call. Split out from a plain String so a safety block and a
 * real response are never confused with each other by callers doing string matching.
 */
sealed interface GenerationResult {
    data class Text(val text: String) : GenerationResult
    data class Blocked(val reason: String) : GenerationResult
}

class GemmaModelManager(private val context: Context) {

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadProgress = MutableStateFlow(0f)
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // LiteRT-LM's Engine owns the loaded model weights; Conversation is the lightweight
    // chat session built on top of it. Engine is expensive to create (up to ~10s per the
    // SDK's own docs), Conversation is cheap — this mirrors that split.
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    // Your app's own content policy layer. LiteRT-LM has no concept of this — it just
    // runs the model — so input/output filtering stays this class's responsibility.
    private val safetyClassifier = SafetyClassifier()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    companion object {
        private const val TAG = "GemmaModelManager"
        private const val MODEL_FILE_NAME = "gemma-3n-E2B-it-int4.litertlm"
        private const val EXPECTED_SIZE_BYTES = 3_800_000_000L // ~3.8GB
        private const val BASE_URL = "https://huggingface.co/ibrahimfonyuy06/gemma3bforaegis/resolve/main/gemma-3n-E2B-it-int4.litertlm"

        // Loaded from BuildConfig, which is populated from local.properties at build time.
        // Never hardcode this — see prior discussion on rotating any token that leaks.

    }

    fun getModelFile(): File = File(context.filesDir, MODEL_FILE_NAME)

    fun isModelInstalled(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() == EXPECTED_SIZE_BYTES
    }

    /**
     * Downloads Gemma 3n to the app's internal storage and emits download updates.
     * Unchanged from before — this part was always correct; the bug was in how the
     * downloaded file got loaded afterward, not in the download itself.
     */
    fun installModel(modelUrl: String = BASE_URL): Flow<DownloadStatus> = flow {
        val targetFile = getModelFile()

        if (isModelInstalled()) {
            emit(DownloadStatus.Success(targetFile))
            return@flow
        }

        var existingLength = if (targetFile.exists()) targetFile.length() else 0L

        if (existingLength > EXPECTED_SIZE_BYTES) {
            targetFile.delete()
            existingLength = 0L
        }

        val requiredSpace = EXPECTED_SIZE_BYTES - existingLength + (500 * 1024 * 1024)
        val availableSpace = context.filesDir.freeSpace
        if (availableSpace < requiredSpace) {
            emit(DownloadStatus.Error("Insufficient storage space. Need ${requiredSpace / (1024 * 1024)}MB"))
            return@flow
        }

        try {
            val requestBuilder = Request.Builder().url(modelUrl)

            if (existingLength > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingLength-")
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    emit(DownloadStatus.Error("Authentication failed. Hugging Face might require a token."))
                    return@flow
                }

                if (existingLength > 0 && response.code != 206) {
                    targetFile.delete()
                    downloadFromBeginning(modelUrl, targetFile, this)
                    return@flow
                }

                if (!response.isSuccessful) {
                    emit(DownloadStatus.Error("Download failed: ${response.code}"))
                    return@flow
                }

                val body = response.body ?: throw java.io.IOException("Empty response body")
                val totalToDownload = if (existingLength > 0) body.contentLength() + existingLength else body.contentLength()

                targetFile.parentFile?.mkdirs()

                FileOutputStream(targetFile, existingLength > 0).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var totalDownloaded = existingLength

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            currentCoroutineContext().ensureActive()

                            output.write(buffer, 0, bytesRead)
                            totalDownloaded += bytesRead

                            if (totalToDownload > 0) {
                                val progress = ((totalDownloaded * 100) / totalToDownload).toInt()
                                emit(DownloadStatus.Progress(progress))
                                _loadProgress.value = totalDownloaded.toFloat() / totalToDownload
                            }
                        }
                    }
                }

                if (targetFile.length() == totalToDownload) {
                    emit(DownloadStatus.Success(targetFile))
                } else {
                    emit(DownloadStatus.Error("File size mismatch after download."))
                }
            }
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.localizedMessage ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadFromBeginning(url: String, targetFile: File, collector: kotlinx.coroutines.flow.FlowCollector<DownloadStatus>) {
        val request = Request.Builder().url(url).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                collector.emit(DownloadStatus.Error("Download failed: ${response.code}"))
                return
            }
            val body = response.body ?: return
            val totalBytes = body.contentLength()
            var bytesDownloaded = 0L

            FileOutputStream(targetFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        if (totalBytes > 0) {
                            val progress = ((bytesDownloaded * 100) / totalBytes).toInt()
                            collector.emit(DownloadStatus.Progress(progress))
                            _loadProgress.value = bytesDownloaded.toFloat() / totalBytes
                        }
                    }
                }
            }
            collector.emit(DownloadStatus.Success(targetFile))
        }
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    /**
     * Initializes the LiteRT-LM Engine against the downloaded .litertlm file and opens a
     * Conversation session. Call this off the main thread — per LiteRT-LM's own docs,
     * engine.initialize() can take up to ~10 seconds. Safe to call more than once; it's a
     * no-op if an engine/conversation already exist.
     */
    suspend fun initializeEngine(): Boolean = withContext(Dispatchers.Default) {
        if (engine != null && conversation != null) return@withContext true

        val modelFile = getModelFile()
        if (!modelFile.exists()) {
            setErrorMessage("Model file not found. Please download the Gemma 3N model first.")
            return@withContext false
        }

        setLoading(true)
        try {
            val engineConfig = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(), // Switch to Backend.GPU() once the manifest's
                // <uses-native-library> entries for libvndksupport.so /
                // libOpenCL.so are in place, if you want GPU accel.
                cacheDir = context.cacheDir.path // Speeds up subsequent loads.
            )

            val newEngine = Engine(engineConfig)
            newEngine.initialize()

            val conversationConfig = ConversationConfig(
                samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 1.0)
            )
            val newConversation = newEngine.createConversation(conversationConfig)

            engine = newEngine
            conversation = newConversation
            _isModelLoaded.value = true
            setErrorMessage(null)
            true
        } catch (e: Exception) {
            setErrorMessage("Failed to initialize LiteRT-LM engine: ${e.localizedMessage}")
            false
        } finally {
            setLoading(false)
        }
    }

    /**
     * Blocks until a complete response is generated. Applies your safety classifier to
     * both the prompt and the finished response — LiteRT-LM has no notion of your app's
     * content policy, so this stays this class's job, same as before.
     */
    suspend fun generate(prompt: String): GenerationResult = withContext(Dispatchers.Default) {
        val inputSafety = safetyClassifier.classify(prompt)
        if (inputSafety.isUnsafe) {
            return@withContext GenerationResult.Blocked("Input contains unsafe content - ${inputSafety.category}")
        }

        val activeConversation = conversation
            ?: return@withContext GenerationResult.Blocked("Engine not initialized. Call initializeEngine() first.")

        try {
            val response = activeConversation.sendMessage(prompt)
            val responseText = response.toString()

            val outputSafety = safetyClassifier.classify(responseText)
            if (outputSafety.isUnsafe) {
                GenerationResult.Blocked("Output contains unsafe content - ${outputSafety.category}")
            } else {
                GenerationResult.Text(responseText)
            }
        } catch (e: Exception) {
            GenerationResult.Blocked("Generation failed: ${e.localizedMessage}")
        }
    }

    /**
     * Streams response chunks as they're generated. The prompt is safety-checked before
     * streaming starts. The accumulated output is safety-checked once streaming finishes;
     * mid-stream chunks aren't checked individually since a partial token fragment isn't
     * meaningful to classify in isolation.
     */
    fun generateStream(prompt: String): Flow<String> = flow {
        val inputSafety = safetyClassifier.classify(prompt)
        if (inputSafety.isUnsafe) {
            emit("[Safety Filter: Input contains unsafe content - ${inputSafety.category}]")
            return@flow
        }

        val activeConversation = conversation
        if (activeConversation == null) {
            emit("[Error: Engine not initialized. Call initializeEngine() first.]")
            return@flow
        }

        val accumulated = StringBuilder()
        activeConversation.sendMessageAsync(prompt).collect { chunk ->
            currentCoroutineContext().ensureActive()
            val chunkText = chunk.toString()
            accumulated.append(chunkText)
            emit(chunkText)
        }

        val outputSafety = safetyClassifier.classify(accumulated.toString())
        if (outputSafety.isUnsafe) {
            emit("\n[Safety Filter: Generation flagged after completion - ${outputSafety.category}]")
        }
    }.flowOn(Dispatchers.Default)

    fun unloadModel() {
        _isModelLoaded.value = false
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
    }

    fun getModelInfo(): GemmaModelInfo {
        val file = getModelFile()
        return GemmaModelInfo(
            name = "Gemma 3N",
            version = "1.0.0",
            size = if (file.exists()) file.length() else 0L,
            isLoaded = _isModelLoaded.value,
            loadedAt = if (_isModelLoaded.value) System.currentTimeMillis() else null
        )
    }
}

data class GemmaModelInfo(
    val name: String,
    val version: String,
    val size: Long,
    val isLoaded: Boolean,
    val loadedAt: Long?
)