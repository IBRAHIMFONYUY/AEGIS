package com.aegis.ai.sampling

import kotlin.math.exp
import kotlin.random.Random

class Sampler(private val vocabSize: Int = 32000) {

    fun sample(logits: FloatArray, params: SamplingParams): Int {
        // Apply temperature scaling
        val scaledLogits = if (params.temperature > 0) {
            logits.map { it / params.temperature }.toFloatArray()
        } else {
            logits.copyOf()
        }

        // Apply softmax to get probabilities
        val probs = softmax(scaledLogits)

        // Apply top-k filtering
        val topKProbs = if (params.topK > 0 && params.topK < probs.size) {
            applyTopK(probs, params.topK)
        } else {
            probs
        }

        // Apply top-p (nucleus) filtering
        val finalProbs = if (params.topP < 1.0f) {
            applyTopP(topKProbs, params.topP)
        } else {
            topKProbs
        }

        // Sample from the final distribution
        return sampleFromDistribution(finalProbs)
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        // exp() returns Double, so it's explicitly converted back to Float here —
        // otherwise this produces a List<Double> and .toFloatArray() won't resolve,
        // since that extension only exists on Collection<Float>.
        val expLogits = logits.map { exp((it - maxLogit).toDouble()).toFloat() }.toFloatArray()
        val sum = expLogits.sum()
        return if (sum > 0) {
            expLogits.map { it / sum }.toFloatArray()
        } else {
            // If all logits are very small, use uniform distribution
            FloatArray(logits.size) { 1f / logits.size }
        }
    }

    private fun applyTopK(probs: FloatArray, k: Int): FloatArray {
        // Get top-k indices and their probabilities
        val indexed = probs.mapIndexed { index, prob -> index to prob }
        val topK = indexed.sortedByDescending { it.second }.take(k)

        // Create new array with only top-k probabilities
        val result = FloatArray(probs.size)
        topK.forEach { (index, prob) ->
            result[index] = prob
        }

        // Renormalize
        val sum = result.sum()
        return if (sum > 0) {
            result.map { it / sum }.toFloatArray()
        } else {
            result
        }
    }

    private fun applyTopP(probs: FloatArray, p: Float): FloatArray {
        // Sort by probability in descending order
        val indexed = probs.mapIndexed { index, prob -> index to prob }
        val sorted = indexed.sortedByDescending { it.second }

        // Find smallest set of tokens with cumulative probability >= p
        var cumulative = 0f
        val selectedIndices = mutableListOf<Int>()

        for ((index, prob) in sorted) {
            if (cumulative < p) {
                selectedIndices.add(index)
                cumulative += prob
            } else {
                break
            }
        }

        // Create new array with only selected probabilities
        val result = FloatArray(probs.size)
        selectedIndices.forEach { index ->
            result[index] = probs[index]
        }

        // Renormalize
        val sum = result.sum()
        return if (sum > 0) {
            result.map { it / sum }.toFloatArray()
        } else {
            result
        }
    }

    private fun sampleFromDistribution(probs: FloatArray): Int {
        val random = Random.nextDouble()
        var cumulative = 0.0

        for (i in probs.indices) {
            cumulative += probs[i]
            if (random <= cumulative) {
                return i
            }
        }

        // Fallback to last index if floating point errors occur
        return probs.size - 1
    }
}

data class SamplingParams(
    val temperature: Float = 1.0f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512
) {
    companion object {
        fun balanced(): SamplingParams = SamplingParams()
        fun creative(): SamplingParams = SamplingParams(temperature = 1.2f, topK = 50, topP = 0.95f)
        fun conservative(): SamplingParams = SamplingParams(temperature = 0.7f, topK = 20, topP = 0.8f)
    }
}