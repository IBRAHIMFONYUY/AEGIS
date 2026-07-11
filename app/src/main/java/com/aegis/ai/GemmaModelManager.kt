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

class GemmaModelManager(
    private val context: Context,
    val safetyClassifier: SafetyClassifier
) {

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadProgress = MutableStateFlow(0f)
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    companion object {
        private const val TAG = "GemmaModelManager"
        private const val MODEL_FILE_NAME = "gemma-3n-E2B-it-int4.litertlm"
        private const val EXPECTED_SIZE_BYTES = 1_000_000_000L 
        private const val BASE_URL = "https://huggingface.co/ibrahimfonyuy06/gemma3bforaegis/resolve/main/gemma-3n-E2B-it-int4.litertlm"
    }

    fun getModelFile(): File = File(context.filesDir, MODEL_FILE_NAME)

    fun isModelInstalled(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() >= EXPECTED_SIZE_BYTES
    }

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

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    private val supportedModelNames = listOf(
        "gemma-3n-E2B-it-int4.litertlm",
        "gemma3.litertlm",
        "gemma.litertlm"
    )

    private fun findExistingModel(): File? {
        val internal = getModelFile()
        if (internal.exists() && internal.length() >= EXPECTED_SIZE_BYTES) return internal

        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val documentDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        
        val explicitPaths = listOf(
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Downloads"),
            File("/storage/emulated/0/Documents")
        )

        val searchDirs = (listOfNotNull(downloadDir, documentDir) + explicitPaths).distinct()

        searchDirs.forEach { dir ->
            try {
                val found = searchFileRecursively(dir)
                if (found != null) return found
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error searching directory %s", dir.absolutePath)
            }
        }

        return null
    }

    private fun searchFileRecursively(directory: File, depth: Int = 0): File? {
        if (depth > 5) return null
        if (!directory.exists() || !directory.isDirectory) return null
        
        val files = directory.listFiles() ?: return null
        
        for (file in files) {
            if (file.isFile) {
                val name = file.name.lowercase()
                if (name.endsWith(".litertlm") && file.length() >= EXPECTED_SIZE_BYTES) {
                    return file
                }
            }
        }
        
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

        if (!modelFile.exists() || modelFile.length() < EXPECTED_SIZE_BYTES) {
            val existing = findExistingModel()
            if (existing != null) {
                try {
                    modelFile = copyModelToInternal(existing)
                } catch (e: Exception) {
                    setErrorMessage("Failed to copy model: ${e.localizedMessage}")
                    return@withContext false
                }
            } else {
                return@withContext false
            }
        }

        setLoading(true)
        try {
            val freeSpace = context.cacheDir.freeSpace
            val useCache = freeSpace > 2_000_000_000L 
            
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

    suspend fun generate(prompt: String): GenerationResult = withContext(Dispatchers.Default) {
        val inputSafety = safetyClassifier.classify(prompt, "ModelInput")
        if (inputSafety.isUnsafe) {
            return@withContext GenerationResult.Blocked("Input contains unsafe content - ${inputSafety.category}")
        }

        val activeConversation = conversation
            ?: return@withContext GenerationResult.Blocked("Engine not initialized.")

        try {
            val response = activeConversation.sendMessage(prompt)
            val responseText = response.toString()

            val outputSafety = safetyClassifier.classify(responseText, "ModelOutput")
            if (outputSafety.isUnsafe) {
                GenerationResult.Blocked("Output contains unsafe content - ${outputSafety.category}")
            } else {
                GenerationResult.Text(responseText)
            }
        } catch (e: Exception) {
            GenerationResult.Blocked("Generation failed: ${e.localizedMessage}")
        }
    }

    fun generateStream(prompt: String): Flow<String> = flow {
        val inputSafety = safetyClassifier.classify(prompt, "ModelInput")
        if (inputSafety.isUnsafe) {
            emit("[Safety Filter: Input contains unsafe content - ${inputSafety.category}]")
            return@flow
        }

        val activeConversation = conversation ?: return@flow

        val accumulated = StringBuilder()
        activeConversation.sendMessageAsync(prompt).collect { chunk ->
            currentCoroutineContext().ensureActive()
            val chunkText = chunk.toString()
            accumulated.append(chunkText)
            emit(chunkText)
        }

        val outputSafety = safetyClassifier.classify(accumulated.toString(), "ModelOutput")
        if (outputSafety.isUnsafe) {
            emit("\n[Safety Filter: Generation flagged - ${outputSafety.category}]")
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
