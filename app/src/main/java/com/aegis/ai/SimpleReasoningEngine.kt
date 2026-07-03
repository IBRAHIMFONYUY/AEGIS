package com.aegis.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SimpleReasoningEngine : ReasoningEngine {
    override suspend fun generateResponse(prompt: String, context: String?): String {
        return "Based on my analysis, this interaction shows signs of potential risk. " +
               "Please verify the sender's identity and avoid sharing sensitive information."
    }

    override fun generateResponseStream(prompt: String, context: String?): Flow<String> {
        return flowOf("Based on my analysis...")
    }

    override suspend fun isModelLoaded(): Boolean = true

    override suspend fun loadModel(): Boolean = true

    override fun getModelInfo(): ModelInfo {
        return ModelInfo("SimpleReasoning", "Heuristic", true, "1.0")
    }
}
