package com.aegis.ai

import kotlinx.coroutines.flow.Flow

/**
 * Core offline reasoning interface for AEGIS AI Guardian system.
 * Supports both single-shot and streaming inference.
 */
interface ReasoningEngine {

    /**
     * Generates a full response for a given prompt using optional context.
     *
     * Must work fully offline if a local model is loaded.
     */
    suspend fun generateResponse(
        prompt: String,
        context: String? = null
    ): String

    /**
     * Streams response tokens/chunks for real-time UI rendering.
     * Useful for chat-like experience.
     */
    fun generateResponseStream(
        prompt: String,
        context: String? = null
    ): Flow<String>

    /**
     * Checks if the local model is loaded and ready for inference.
     */
    suspend fun isModelLoaded(): Boolean

    /**
     * Loads the AI model into memory (ONNX / TFLite / GGUF etc).
     *
     * Returns true if successfully loaded.
     */
    suspend fun loadModel(): Boolean

    /**
     * Returns metadata about the current model.
     * Useful for debugging and system diagnostics.
     */
    fun getModelInfo(): ModelInfo

    /**
     * OPTIONAL SAFETY CHECK (NEW)
     * Ensures engine is ready before inference.
     */
    suspend fun ensureReady(): Boolean = isModelLoaded()
}