package com.aegis.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
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

class GemmaModelManager(private val context: Context) {

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadProgress = MutableStateFlow(0f)
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()

    companion object {
        private const val TAG = "GemmaModelManager"
        private const val MODEL_FILE_NAME = "gemma-3n-E2B-it-int4.litertlm"
        private const val EXPECTED_SIZE_BYTES = 3_500_000_000L // ~1.5GB
        private const val BASE_URL = "https://huggingface.co"
        private const val HF_TOKEN = "" // Set this if authentication is required
    }

    fun getModelFile(): File = File(context.filesDir, MODEL_FILE_NAME)

    fun isModelInstalled(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() == EXPECTED_SIZE_BYTES
    }

    /**
     * Downloads Gemma 3n to the app's internal storage and emits download updates.
     */
    fun installModel(modelUrl: String = "$BASE_URL/$MODEL_FILE_NAME"): Flow<DownloadStatus> = flow {
        val targetFile = getModelFile()
        
        if (isModelInstalled()) {
            emit(DownloadStatus.Success(targetFile))
            return@flow
        }

        val existingLength = if (targetFile.exists()) targetFile.length() else 0L
        
        // Storage check
        val requiredSpace = EXPECTED_SIZE_BYTES - existingLength + (500 * 1024 * 1024)
        val availableSpace = context.filesDir.freeSpace
        if (availableSpace < requiredSpace) {
            emit(DownloadStatus.Error("Insufficient storage space. Need ${requiredSpace / (1024 * 1024)}MB"))
            return@flow
        }

        try {
            val requestBuilder = Request.Builder().url(modelUrl)
            if (HF_TOKEN.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $HF_TOKEN")
            }
            
            if (existingLength > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingLength-")
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    emit(DownloadStatus.Error("Authentication failed. Hugging Face might require a token."))
                    return@flow
                }
                
                if (existingLength > 0 && response.code != 206) {
                    // Server doesn't support Range, restart
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
                
                if (targetFile.length() == EXPECTED_SIZE_BYTES) {
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
        val request = Request.Builder().url(url).apply {
            if (HF_TOKEN.isNotEmpty()) addHeader("Authorization", "Bearer $HF_TOKEN")
        }.build()

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

    fun setModelLoaded(loaded: Boolean) {
        _isModelLoaded.value = loaded
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun unloadModel() {
        _isModelLoaded.value = false
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
