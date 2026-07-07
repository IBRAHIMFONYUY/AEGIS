package com.aegis.ai.sampling

class Sampler(private val vocabSize: Int) {
    fun sample(logits: FloatArray, params: SamplingParams): Int {
        return logits.indices.maxByOrNull { logits[it] } ?: 0
    }
}

data class SamplingParams(
    val temperature: Float = 1.0f,
    val topK: Int = 40,
    val topP: Float = 0.9f
) {
    companion object {
        fun balanced(): SamplingParams = SamplingParams()
    }
}
