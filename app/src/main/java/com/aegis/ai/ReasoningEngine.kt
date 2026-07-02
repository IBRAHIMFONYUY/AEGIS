package com.aegis.ai

import kotlinx.coroutines.flow.Flow

interface ReasoningEngine {
    suspend fun generateResponse(prompt: String, context: String? = null): String
    fun generateResponseStream(prompt: String, context: String? = null): Flow<String>
    suspend fun isModelLoaded(): Boolean
    suspend fun loadModel(): Boolean
    fun getModelInfo(): ModelInfo
}
