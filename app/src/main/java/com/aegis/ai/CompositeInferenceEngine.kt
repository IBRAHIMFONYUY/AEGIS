package com.aegis.ai

import android.content.Context

class CompositeInferenceEngine(
    context: Context,
    private val modelManager: ModelManager = ModelManager(context)
) : InferenceEngine {

    private val onnxEngine = OnnxInference(context, modelManager)
    private val tfliteEngine = TFLiteInference(context, modelManager)

    override suspend fun classify(text: String, modelType: String, metadata: Map<String, String>): Float {
        val config = modelManager.getModelConfig(modelType) ?: return 0f
        return when (config.engineType) {
            ModelType.ONNX -> onnxEngine.classify(text, modelType, metadata)
            ModelType.TFLITE -> tfliteEngine.classify(text, modelType, metadata)
            ModelType.MLKIT -> 0f
        }
    }

    override suspend fun analyzeText(text: String, modelType: String, metadata: Map<String, String>): Map<String, Float> {
        val config = modelManager.getModelConfig(modelType) ?: return emptyMap()
        return when (config.engineType) {
            ModelType.ONNX -> onnxEngine.analyzeText(text, modelType, metadata)
            ModelType.TFLITE -> tfliteEngine.analyzeText(text, modelType, metadata)
            ModelType.MLKIT -> emptyMap()
        }
    }

    override suspend fun isModelLoaded(modelType: String): Boolean {
        val config = modelManager.getModelConfig(modelType) ?: return false
        return when (config.engineType) {
            ModelType.ONNX -> onnxEngine.isModelLoaded(modelType)
            ModelType.TFLITE -> tfliteEngine.isModelLoaded(modelType)
            ModelType.MLKIT -> true
        }
    }

    override suspend fun loadModel(modelType: String): Boolean {
        val config = modelManager.getModelConfig(modelType) ?: return false
        return when (config.engineType) {
            ModelType.ONNX -> onnxEngine.loadModel(modelType)
            ModelType.TFLITE -> tfliteEngine.loadModel(modelType)
            ModelType.MLKIT -> true
        }
    }

    override fun getAvailableModels(): List<ModelInfo> = modelManager.getAvailableModels()

    fun release() {
        onnxEngine.release()
        tfliteEngine.release()
    }
}
