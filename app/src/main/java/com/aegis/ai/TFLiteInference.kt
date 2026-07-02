package com.aegis.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteInference(
    private val context: Context,
    private val modelManager: ModelManager
) : InferenceEngine {

    private val interpreters = mutableMapOf<String, Interpreter>()

    override suspend fun classify(text: String, modelType: String): Float =
        withContext(Dispatchers.IO) {
            try {
                val interpreter = getOrCreateInterpreter(modelType) ?: return@withContext 0f
                val inputBuffer = preprocessForTFLite(text, interpreter)
                val output = Array(1) { FloatArray(2) }
                interpreter.run(inputBuffer, output)
                output[0][1]
            } catch (e: Exception) {
                0f
            }
        }

    override suspend fun analyzeText(text: String, modelType: String): Map<String, Float> =
        withContext(Dispatchers.IO) {
            try {
                val interpreter = getOrCreateInterpreter(modelType) ?: return@withContext emptyMap()
                val inputBuffer = preprocessForTFLite(text, interpreter)
                val output = Array(1) { FloatArray(5) }
                interpreter.run(inputBuffer, output)
                mapOf(
                    "toxic" to output[0][0],
                    "severe_toxic" to output[0][1],
                    "obscene" to output[0][2],
                    "threat" to output[0][3],
                    "insult" to output[0][4]
                )
            } catch (e: Exception) {
                emptyMap()
            }
        }

    override suspend fun isModelLoaded(modelType: String): Boolean =
        interpreters.containsKey(modelType)

    override suspend fun loadModel(modelType: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                getOrCreateInterpreter(modelType)
                true
            } catch (e: Exception) {
                false
            }
        }

    override fun getAvailableModels(): List<ModelInfo> {
        return modelManager.getModelsByEngine(ModelType.TFLITE).map {
            ModelInfo(
                name = it.type,
                type = "TFLite",
                isLoaded = interpreters.containsKey(it.type),
                version = it.version
            )
        }
    }

    private fun getOrCreateInterpreter(modelType: String): Interpreter? {
        if (interpreters.containsKey(modelType)) return interpreters[modelType]
        val modelPath = modelManager.getModelPath(modelType) ?: return null
        return try {
            val buffer = loadModelFile(modelPath)
            val interpreter = Interpreter(buffer)
            interpreters[modelType] = interpreter
            modelManager.markLoaded(modelType)
            interpreter
        } catch (e: Exception) {
            null
        }
    }

    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val file = java.io.File(modelPath)
        return FileInputStream(file).use { inputStream ->
            val channel = inputStream.channel
            channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
        }
    }

    private fun preprocessForTFLite(text: String, interpreter: Interpreter): ByteBuffer {
        val inputShape = interpreter.getInputTensor(0).shape()
        val maxLength = inputShape.getOrElse(1) { 384 }
        val inputSize = maxLength * 4
        val buffer = ByteBuffer.allocateDirect(inputSize)
        buffer.order(ByteOrder.nativeOrder())
        val words = text.lowercase().split("\\s+".toRegex()).take(maxLength)
        words.forEachIndexed { i, word ->
            buffer.putFloat(word.hashCode().toFloat() / 1000f)
        }
        for (i in words.size until maxLength) {
            buffer.putFloat(0f)
        }
        buffer.rewind()
        return buffer
    }

    fun release() {
        interpreters.values.forEach { it.close() }
        interpreters.clear()
    }
}
