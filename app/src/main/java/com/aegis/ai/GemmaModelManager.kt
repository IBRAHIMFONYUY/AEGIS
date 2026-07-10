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
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
        private const val EXPECTED_SIZE_BYTES = 1_000_000_000L // 1GB threshold to catch all Gemma variants
        private const val BASE_URL = "https://huggingface.co/ibrahimfonyuy06/gemma3bforaegis/resolve/main/gemma-3n-E2B-it-int4.litertlm"

        // Loaded from BuildConfig, which is populated from local.properties at build time.
        // Never hardcode this — see prior discussion on rotating any token that leaks.

    }

    fun getModelFile(): File = File(context.filesDir, MODEL_FILE_NAME)

    fun isModelInstalled(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() >= EXPECTED_SIZE_BYTES
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

    private val supportedModelNames = listOf(
        "gemma-3n-E2B-it-int4.litertlm",
        "gemma3.litertlm",
        "gemma.litertlm",
        "gemma-2b-it.litertlm",
        "gemma-2b-it-int4.litertlm",
        "gemma-1.1-2b-it.litertlm",
        "gemma-3b-it.litertlm",
        "gemma-3b-it-int4.litertlm"
    )

    private fun findExistingModel(): File? {
        // 1. Internal storage check first
        val internal = getModelFile()
        if (internal.exists() && internal.length() >= EXPECTED_SIZE_BYTES) return internal

        // 2. Search common public directories
        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val documentDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        
        // Also check standard paths explicitly as fallback
        val storagePath = android.os.Environment.getExternalStorageDirectory().path
        val explicitPaths = listOf(
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Downloads"),
            File("/storage/emulated/0/Documents"),
            File("/storage/emulated/0/Models"),
            File("$storagePath/Download"),
            File("$storagePath/Downloads"),
            File("$storagePath/Documents"),
            File("$storagePath/Models")
        )

        val searchDirs = (listOfNotNull(downloadDir, documentDir, android.os.Environment.getExternalStorageDirectory()) + explicitPaths).distinct()
        Timber.tag(TAG).d("Searching for model in: %s", searchDirs.joinToString { it.absolutePath })

        searchDirs.forEach { dir ->
            try {
                val found = searchFileRecursively(dir)
                if (found != null) {
                    Timber.tag(TAG).i("Found existing model at: %s", found.absolutePath)
                    return found
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error searching directory %s", dir.absolutePath)
            }
        }

        return null
    }

    private fun searchFileRecursively(directory: File, depth: Int = 0): File? {
        if (depth > 5) return null // Slightly deeper search
        if (!directory.exists() || !directory.isDirectory) return null
        
        val files = directory.listFiles()
        if (files == null) {
            Timber.tag(TAG).w("Cannot list files in %s (access denied?)", directory.absolutePath)
            return null
        }
        
        // Priority 1: Exact matches, contains "gemma", or ANY .litertlm/.tflite file
        for (file in files) {
            if (file.isFile) {
                val name = file.name.lowercase()
                val isModelFile = name.endsWith(".litertlm") || 
                                 name.endsWith(".tflite") ||
                                 supportedModelNames.any { it.lowercase() == name }
                
                if (isModelFile && file.length() >= EXPECTED_SIZE_BYTES) {
                    return file
                }
            }
        }
        
        // Priority 2: Subdirectories
        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                val found = searchFileRecursively(file, depth + 1)
                if (found != null) return found
            }
        }
        
        return null
    }

    private fun copyModelToInternal(source: File): File {
        val destination = getModelFile()
        if (destination.exists()) destination.delete()
        source.copyTo(destination)
        return destination
    }

    fun importModel(uri: Uri): Boolean {
        _isLoading.value = true
        return try {
            val availableSpace = context.filesDir.freeSpace
            if (availableSpace < EXPECTED_SIZE_BYTES + (500 * 1024 * 1024)) {
                setErrorMessage("Insufficient storage to import model. Need at least 4.5GB free.")
                return false
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                val destination = getModelFile()
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            _isLoading.value = false
        }
    }
    suspend fun initializeEngine(): Boolean = withContext(Dispatchers.Default) {
        if (engine != null && conversation != null) return@withContext true

        var modelFile = getModelFile()
        Timber.tag(TAG).d("Initializing engine. Internal model exists: %s", modelFile.exists())

        if (!modelFile.exists() || modelFile.length() < EXPECTED_SIZE_BYTES) {
            Timber.tag(TAG).d("Internal model missing or incomplete. Searching external storage...")
            val existing = findExistingModel()
            if (existing != null) {
                // If we found it externally and have permission, use it directly to save space
                // instead of copying it to internal storage.
                Timber.tag(TAG).d("Using existing model in-place at %s", existing.absolutePath)
                modelFile = existing
            } else {
                Timber.tag(TAG).w("Model not found in Downloads or Documents.")
                setErrorMessage("Gemma model not found. Please download or choose a model file.")
                return@withContext false
            }
        }

        setLoading(true)
        try {
            // Check if we have enough space for the engine's cache.
            // LiteRT-LM can use significant space for compiled model caching.
            val freeSpace = context.cacheDir.freeSpace
            // Be very conservative: Only use cache if we have > 2GB free
            // This prevents the "filling up remaining space" issue.
            val useCache = freeSpace > 2_000_000_000L 
            
            Timber.tag(TAG).d("Free space: %d bytes. Use cache: %s", freeSpace, useCache)
            
            val engineConfig = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                cacheDir = if (useCache) context.cacheDir.path else null
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