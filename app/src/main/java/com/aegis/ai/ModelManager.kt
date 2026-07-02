package com.aegis.ai

import android.content.Context
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ModelManager(private val context: Context) {

    private val loadedModels = ConcurrentHashMap<String, Boolean>()
    private val modelConfigs = listOf(
        ModelConfig("scam_detection", "scam_model.onnx", ModelType.ONNX, "1.0"),
        ModelConfig("toxicity", "toxicity_model.tflite", ModelType.TFLITE, "1.0"),
        ModelConfig("misinformation", "misinformation_model.onnx", ModelType.ONNX, "1.0"),
        ModelConfig("hate_speech", "hate_speech_model.onnx", ModelType.ONNX, "1.0"),
        ModelConfig("privacy", "privacy_model.onnx", ModelType.ONNX, "1.0"),
        ModelConfig("llm_reasoning", "phi3_quantized.onnx", ModelType.ONNX, "1.0"),
        ModelConfig("ocr", null, ModelType.MLKIT, "1.0"),
        ModelConfig("vision", "vision_model.tflite", ModelType.TFLITE, "1.0"),
        ModelConfig("deepfake", "deepfake_model.onnx", ModelType.ONNX, "1.0"),
        ModelConfig("whisper", "whisper_quantized.onnx", ModelType.ONNX, "1.0")
    )

    fun getModelConfig(modelType: String): ModelConfig? {
        return modelConfigs.find { it.type == modelType }
    }

    fun getModelPath(modelType: String): String? {
        val config = getModelConfig(modelType) ?: return null
        val fileName = config.fileName ?: return null
        val file = File(context.filesDir, "models/$fileName")
        return if (file.exists()) file.absolutePath else null
    }

    fun isModelDownloaded(modelType: String): Boolean {
        val config = getModelConfig(modelType) ?: return false
        if (config.fileName == null) return true
        return File(context.filesDir, "models/${config.fileName}").exists()
    }

    fun markLoaded(modelType: String) {
        loadedModels[modelType] = true
    }

    fun markUnloaded(modelType: String) {
        loadedModels[modelType] = false
    }

    fun isLoaded(modelType: String): Boolean = loadedModels[modelType] ?: false

    fun getAvailableModels(): List<ModelInfo> {
        return modelConfigs.map { config ->
            ModelInfo(
                name = config.type,
                type = config.engineType.name,
                isLoaded = isLoaded(config.type),
                version = config.version
            )
        }
    }

    fun getModelsByEngine(engineType: ModelType): List<ModelConfig> {
        return modelConfigs.filter { it.engineType == engineType }
    }
}

data class ModelConfig(
    val type: String,
    val fileName: String?,
    val engineType: ModelType,
    val version: String
)

enum class ModelType {
    ONNX,
    TFLITE,
    MLKIT
}
