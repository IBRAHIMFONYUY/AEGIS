package com.aegis.ai

import android.content.Context
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to handle Gemini AI operations across the app.
 */
@Singleton
class GeminiService @Inject constructor(
    private val geminiAIManager: GeminiAIManager
) {
    suspend fun analyzeText(text: String, context: String = ""): ThreatAnalysisResult {
        return geminiAIManager.analyzeThreat(text, context)
    }

    suspend fun analyzeConversation(
        messages: List<String>,
        currentMessage: String,
        senderInfo: String = ""
    ): ConversationAnalysisResult {
        return geminiAIManager.analyzeConversationHistory(messages, currentMessage, senderInfo)
    }

    fun streamResponse(prompt: String): Flow<String> {
        return geminiAIManager.generateResponseStream(prompt)
    }

    suspend fun quickInference(prompt: String): String {
        return geminiAIManager.generateResponse(prompt)
    }
}
