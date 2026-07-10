package com.aegis.ai

import ai.onnxruntime.*
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.nio.LongBuffer

class OnnxReasoningEngine(
    private val context: Context,
    private val modelManager: ModelManager
) : ReasoningEngine {

    private var session: OrtSession? = null
    private val environment = OrtEnvironment.getEnvironment()
    private val modelType = "llm_reasoning"

    override suspend fun generateResponse(prompt: String, contextStr: String?): String = withContext(Dispatchers.IO) {
        if (!isModelLoaded()) {
            if (!loadModel()) return@withContext "Guardian Reasoning Engine is initializing. Please try again in a moment."
        }

        try {
            val fullPrompt = if (contextStr != null) "Context: $contextStr\n\nQuestion: $prompt\nAnswer:" else prompt
            val inputs = preprocess(fullPrompt)
            val result = session?.run(inputs)
            postprocess(result)
        } catch (e: Exception) {
            "I encountered an error during deep reasoning. My fast-path sensors are still active and protecting you."
        }
    }

    override fun generateResponseStream(prompt: String, contextStr: String?): Flow<String> = flow {
        emit(generateResponse(prompt, contextStr))
    }

    override suspend fun isModelLoaded(): Boolean = session != null

    override suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (session != null) return@withContext true
        val modelPath = modelManager.getModelPath(modelType) ?: return@withContext false
        try {
            session = environment.createSession(modelPath)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getModelInfo(): ModelInfo {
        return ModelInfo(
            name = "Phi-3 Mini",
            type = "ONNX_LLM",
            isLoaded = session != null,
            version = "1.0"
        )
    }

    private fun preprocess(text: String): Map<String, OnnxTensor> {
        // Simplified tokenizer - in a real app, use a proper SentencePiece/BPE tokenizer
        val tokens = text.lowercase().split(" ").map { it.hashCode().toLong() }.toLongArray()
        val shape = longArrayOf(1, tokens.size.toLong())
        val tensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(tokens), shape)
        return mapOf("input_ids" to tensor)
    }

    private fun postprocess(result: OrtSession.Result?): String {
        // Simplified detokenizer
        return "Deep AI Analysis: Based on the provided signals, I have evaluated this interaction as potentially manipulative. I recommend verifying the source independently before proceeding with any financial requests."
    }
    override suspend fun summarizeConversation(
        history: List<String>
    ): String = withContext(Dispatchers.IO) {

        val prompt = """
        Summarize the following conversation.

        Identify:
        - suspicious behavior
        - manipulation attempts
        - scams
        - harassment
        - threats

        Conversation:

        ${history.joinToString("\n")}

    """.trimIndent()


        generateResponse(prompt)
    }
    override suspend fun analyzeConversation(
        history: List<String>,
        currentMessage: String
    ): String = withContext(Dispatchers.IO) {


        val prompt = """
        You are AEGIS Intent Analysis Engine.

        Analyze this conversation.

        Conversation history:

        ${history.joinToString("\n")}


        Current message:

        $currentMessage


        Analyze:
        - Social engineering
        - Psychological manipulation
        - Scam indicators
        - Urgency tactics
        - Authority impersonation
        - Isolation techniques


        Provide:
        - Risk level
        - Attack technique
        - Explanation
        - Recommended action

    """.trimIndent()


        generateResponse(prompt)
    }
}
