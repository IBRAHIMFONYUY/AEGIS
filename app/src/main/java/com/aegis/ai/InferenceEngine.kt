package com.aegis.ai

interface InferenceEngine {
    suspend fun classify(text: String, modelType: String): Float
    suspend fun analyzeText(text: String, modelType: String): Map<String, Float>
    suspend fun isModelLoaded(modelType: String): Boolean
    suspend fun loadModel(modelType: String): Boolean
    fun getAvailableModels(): List<ModelInfo>
}

data class ModelInfo(
    val name: String,
    val type: String,
    val isLoaded: Boolean,
    val version: String,
    val sizeBytes: Long = 0
)
