package com.aegis.ai

import android.content.Context
import ai.onnxruntime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import java.nio.LongBuffer

class OnnxInference(
    private val context: Context,
    private val modelManager: ModelManager
) : InferenceEngine {

    private val sessions = mutableMapOf<String, OrtSession>()
    private val environment = OrtEnvironment.getEnvironment()

    override suspend fun classify(text: String, modelType: String): Float =
        withContext(Dispatchers.IO) {
            try {
                val session = getOrCreateSession(modelType) ?: return@withContext 0f
                val inputMap = preprocessText(text, session)
                val results = session.run(inputMap)
                postprocessClassification(results)
            } catch (e: Exception) {
                0f
            }
        }

    override suspend fun analyzeText(text: String, modelType: String): Map<String, Float> =
        withContext(Dispatchers.IO) {
            emptyMap()
        }

    override suspend fun isModelLoaded(modelType: String): Boolean =
        sessions.containsKey(modelType)

    override suspend fun loadModel(modelType: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                getOrCreateSession(modelType)
                true
            } catch (e: Exception) {
                false
            }
        }

    override fun getAvailableModels(): List<ModelInfo> {
        return modelManager.getModelsByEngine(ModelType.ONNX).map {
            ModelInfo(
                name = it.type,
                type = "ONNX",
                isLoaded = sessions.containsKey(it.type),
                version = it.version
            )
        }
    }

    private fun getOrCreateSession(modelType: String): OrtSession? {
        if (sessions.containsKey(modelType)) return sessions[modelType]
        val modelPath = modelManager.getModelPath(modelType) ?: return null
        return try {
            val session = environment.createSession(modelPath)
            sessions[modelType] = session
            modelManager.markLoaded(modelType)
            session
        } catch (e: Exception) {
            null
        }
    }

    private fun preprocessText(text: String, session: OrtSession): Map<String, OnnxTensor> {
        val inputName = session.inputNames.iterator().next()
        val inputInfo = session.inputInfo[inputName]?.info as? TensorInfo
        val shape = inputInfo?.shape ?: longArrayOf(1L, 384L)
        val maxLength = shape.getOrElse(1) { 384L }.toInt()

        val inputIds = LongArray(maxLength) { 0L }
        val attentionMask = LongArray(maxLength) { 0L }
        val tokenTypeIds = LongArray(maxLength) { 0L }

        val words = text.lowercase().split("\\s+".toRegex()).take(maxLength - 2)
        inputIds[0] = 101L
        attentionMask[0] = 1L
        words.forEachIndexed { index, _ ->
            inputIds[index + 1] = 1L
            attentionMask[index + 1] = 1L
        }
        if (words.size + 1 < maxLength) {
            inputIds[words.size + 1] = 102L
            attentionMask[words.size + 1] = 1L
        }

        return try {
            val inputTensor = OnnxTensor.createTensor(
                environment,
                LongBuffer.wrap(inputIds),
                longArrayOf(1L, maxLength.toLong())
            )
            val attentionTensor = OnnxTensor.createTensor(
                environment,
                LongBuffer.wrap(attentionMask),
                longArrayOf(1L, maxLength.toLong())
            )
            val tokenTensor = OnnxTensor.createTensor(
                environment,
                LongBuffer.wrap(tokenTypeIds),
                longArrayOf(1L, maxLength.toLong())
            )
            mapOf(
                "input_ids" to inputTensor,
                "attention_mask" to attentionTensor,
                "token_type_ids" to tokenTensor
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun postprocessClassification(results: OrtSession.Result): Float {
        return try {
            val output = results.get("logits").orElse(null)?.value
            if (output is Array<*>) {
                val logits = output[0] as? FloatArray ?: return 0f
                if (logits.size >= 2) {
                    val maxVal = logits.maxOrNull() ?: 0f
                    val expSum = logits.sumOf { kotlin.math.exp(it.toDouble() - maxVal.toDouble()) }
                    (kotlin.math.exp(logits[1].toDouble() - maxVal.toDouble()) / expSum).toFloat()
                } else 0f
            } else 0f
        } catch (e: Exception) {
            0f
        }
    }

    fun release() {
        sessions.values.forEach { it.close() }
        sessions.clear()
    }
}
