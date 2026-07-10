package com.aegis.ai.runtime

import com.aegis.ai.LiteRTInterpreterOps
import com.aegis.ai.safety.SafetyClassifier
import com.aegis.ai.sampling.SamplingParams
import com.aegis.ai.tokenizer.BPETokenizer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LLMRuntime(
    private val interpreter: LiteRTInterpreterOps,
    private val tokenizer: BPETokenizer,
    private val sampler: com.aegis.ai.sampling.Sampler,
    private val safetyClassifier: SafetyClassifier,
    private val config: LLMConfig
) {
    fun initialize() {
        // Prepare kv cache or other runtime components if needed
    }

    fun classifySafety(text: String): SafetyClassifier.SafetyResult {
        return safetyClassifier.classify(text)
    }

    fun generate(prompt: String, params: SamplingParams): String {
        // Check input safety
        val inputSafety = safetyClassifier.classify(prompt)
        if (inputSafety.isUnsafe) {
            return "[Safety Filter: Input contains unsafe content - ${inputSafety.category}]"
        }

        // Tokenize input
        val inputTokens = tokenizer.encode(prompt)
        val generatedTokens = mutableListOf<Int>()

        // Initialize with input tokens
        var currentTokens = inputTokens.toMutableList()

        // Autoregressive generation loop
        var generatedText = ""
        var iterations = 0
        val maxIterations = params.maxTokens

        while (iterations < maxIterations) {
            // Truncate context to the model's max sequence length if needed
            if (currentTokens.size > config.maxSeqLen) {
                currentTokens = currentTokens.takeLast(config.maxSeqLen).toMutableList()
            }

            // Run interpreter to get logits for next token
            val logits = runInference(currentTokens)

            // Sample next token using sampler
            val nextToken = sampler.sample(logits, params)

            // Check for end-of-sequence token
            if (nextToken == tokenizer.eosTokenId) {
                break
            }

            // Add token to generated sequence
            generatedTokens.add(nextToken)
            currentTokens.add(nextToken)

            // Decode partial result for safety check
            val partialText = tokenizer.decode(generatedTokens.toIntArray())

            // Check output safety periodically
            if (iterations % 5 == 0) {
                val safetyResult = safetyClassifier.classify(partialText)
                if (safetyResult.isUnsafe) {
                    return "[Safety Filter: Generation stopped due to unsafe content - ${safetyResult.category}]"
                }
            }

            generatedText = partialText
            iterations++
        }

        // Final safety check on complete output
        val finalSafety = safetyClassifier.classify(generatedText)
        if (finalSafety.isUnsafe) {
            return "[Safety Filter: Output contains unsafe content - ${finalSafety.category}]"
        }

        return generatedText.ifEmpty { "Unable to generate response." }
    }

    fun generateStream(prompt: String, params: SamplingParams, tokenCallback: (String) -> Unit) {
        // Check input safety
        val inputSafety = safetyClassifier.classify(prompt)
        if (inputSafety.isUnsafe) {
            tokenCallback("[Safety Filter: Input contains unsafe content - ${inputSafety.category}]")
            return
        }

        // Tokenize input
        val inputTokens = tokenizer.encode(prompt)
        val generatedTokens = mutableListOf<Int>()

        // Initialize with input tokens
        var currentTokens = inputTokens.toMutableList()

        // Autoregressive generation loop with streaming
        var iterations = 0
        val maxIterations = params.maxTokens
        var lastDecodedLength = 0

        while (iterations < maxIterations) {
            // Truncate context to the model's max sequence length if needed
            if (currentTokens.size > config.maxSeqLen) {
                currentTokens = currentTokens.takeLast(config.maxSeqLen).toMutableList()
            }

            // Run interpreter to get logits for next token
            val logits = runInference(currentTokens)

            // Sample next token using sampler
            val nextToken = sampler.sample(logits, params)

            // Check for end-of-sequence token
            if (nextToken == tokenizer.eosTokenId) {
                break
            }

            // Add token to generated sequence
            generatedTokens.add(nextToken)
            currentTokens.add(nextToken)

            // Decode and stream new content
            val partialText = tokenizer.decode(generatedTokens.toIntArray())
            if (partialText.length > lastDecodedLength) {
                val newContent = partialText.substring(lastDecodedLength)
                tokenCallback(newContent)
                lastDecodedLength = partialText.length
            }

            // Check output safety periodically
            if (iterations % 5 == 0) {
                val safetyResult = safetyClassifier.classify(partialText)
                if (safetyResult.isUnsafe) {
                    tokenCallback("[Safety Filter: Generation stopped due to unsafe content]")
                    return
                }
            }

            iterations++
        }
    }

    private fun runInference(tokens: List<Int>): FloatArray {
        // Run the TFLite interpreter with the current token sequence
        // Prepare input tensor with token IDs
        val inputBuffer = ByteBuffer.allocateDirect(tokens.size * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        tokens.forEach { token ->
            inputBuffer.putInt(token)
        }
        inputBuffer.rewind()

        // Prepare output tensor for logits
        val outputBuffer = ByteBuffer.allocateDirect(config.vocabSize * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        // Run inference
        try {
            interpreter.run(inputBuffer, outputBuffer)

            // Extract logits
            val logits = FloatArray(config.vocabSize)
            outputBuffer.rewind()
            for (i in logits.indices) {
                logits[i] = outputBuffer.float
            }

            return logits
        } catch (e: Exception) {
            throw RuntimeException("TFLite inference failed: ${e.message}", e)
        }
    }

    fun getStats(): RuntimeStats {
        // Calculate real runtime statistics
        val startTime = System.nanoTime()

        // Run a quick benchmark inference
        val testTokens = listOf(1, 2, 3)
        runInference(testTokens)

        val endTime = System.nanoTime()
        val latencyMs = (endTime - startTime) / 1_000_000.0f

        // Estimate tokens per second based on latency
        val tokensPerSecond = if (latencyMs > 0) (1000.0f / latencyMs).toInt() else 0

        // Estimate memory usage (this is an approximation)
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsageMb = (usedMemory / (1024 * 1024)).toInt()

        return RuntimeStats(
            avgLatencyMs = latencyMs,
            tokensPerSecond = tokensPerSecond,
            memoryUsageMb = memoryUsageMb
        )
    }

    fun close() {
        interpreter.close()
    }

    fun cleanup() {
        close()
    }
}

data class LLMConfig(
    val vocabSize: Int,
    val maxSeqLen: Int,
    val numLayers: Int,
    val numHeads: Int,
    val headDim: Int
)

data class GenerationResult(
    val text: String,
    val confidence: Float
)

data class RuntimeStats(
    val avgLatencyMs: Float,
    val tokensPerSecond: Int,
    val memoryUsageMb: Int
)

object LLMRuntimeFactory {
    fun create(
        interpreter: com.aegis.ai.LiteRTInterpreterOps,
        tokenizer: BPETokenizer,
        sampler: com.aegis.ai.sampling.Sampler,
        safetyClassifier: SafetyClassifier,
        config: LLMConfig
    ): LLMRuntime {
        return LLMRuntime(interpreter, tokenizer, sampler, safetyClassifier, config)
    }

    /**
     * Creates an LLMRuntime backed by a fake interpreter — no TFLite model, no native
     * calls. It's a real, working interpreter in the sense that it satisfies the
     * LiteRTInterpreter contract end to end (correct buffer types/sizes, deterministic
     * output, actual termination), just with fabricated logits instead of a trained
     * model. Good for unit tests, previewing UI/download flows, and exercising
     * generate()/generateStream() without a 3.5GB model on disk.
     *
     * Behavior, by design:
     * - Deterministic: the same running token context always yields the same logits,
     *   so tests are reproducible.
     * - Stateless: this interpreter instance may be reused across multiple generate()
     *   calls (LLMRuntime caches it), so it derives everything from the input buffer
     *   passed to run() rather than mutable instance state — no risk of a counter from
     *   a previous call bleeding into the next one, and it's safe under concurrent use.
     * - Actually terminates: forces the EOS token at a predictable interval and, as a
     *   hard backstop, once the context reaches maxSequenceLength — so callers can
     *   verify generation stops, instead of every test run silently maxing out at
     *   params.maxTokens with pure noise.
     *
     * NOT for production inference — the generated text is structurally valid but
     * semantically meaningless.
     *
     * @param forcedEosEveryNTokens how often (in input-token-count terms) the mock
     *   forces EOS, so tests can assert on termination without waiting for the full
     *   context window. Defaults to a fraction of maxSequenceLength.
     */
    fun createWithMockInterpreter(
        tokenizer: BPETokenizer,
        kvCache: com.aegis.ai.cache.KVCache,
        sampler: com.aegis.ai.sampling.Sampler,
        safetyClassifier: SafetyClassifier,
        vocabSize: Int,
        maxSequenceLength: Int,
        forcedEosEveryNTokens: Int = maxOf(4, maxSequenceLength / 8)
    ): LLMRuntime {
        require(vocabSize > 0) { "vocabSize must be positive" }
        require(maxSequenceLength > 0) { "maxSequenceLength must be positive" }

        val mockInterpreter = object : com.aegis.ai.LiteRTInterpreterOps {

            private fun writeLogitsFor(inputTokenCount: Int, seed: Long, outBuffer: java.nio.ByteBuffer) {
                val forceEos = inputTokenCount > 0 &&
                        (inputTokenCount >= maxSequenceLength || inputTokenCount % forcedEosEveryNTokens == 0)

                val nextTokenId = if (forceEos) {
                    (tokenizer.eosTokenId % vocabSize + vocabSize) % vocabSize
                } else {
                    ((seed % vocabSize) + vocabSize) % vocabSize
                }.toInt()

                outBuffer.order(java.nio.ByteOrder.nativeOrder())
                outBuffer.rewind()
                // Peaked, deterministic distribution: nextTokenId gets the highest logit
                // and others decay with distance from it. Unlike flat noise, this behaves
                // sensibly under both greedy/argmax sampling and temperature/top-k/nucleus
                // sampling, and it's easy to assert "the chosen token was near X" in tests.
                for (i in 0 until vocabSize) {
                    val distance = kotlin.math.abs(i - nextTokenId)
                    outBuffer.putFloat(-distance.toFloat())
                }
                outBuffer.rewind()
            }

            override fun run(input: Any, output: Any) {
                val outBuffer = output as? java.nio.ByteBuffer
                    ?: throw IllegalArgumentException("Mock interpreter expects a ByteBuffer output, got ${output::class.simpleName}")
                val inBuffer = input as? java.nio.ByteBuffer
                    ?: throw IllegalArgumentException("Mock interpreter expects a ByteBuffer input, got ${input::class.simpleName}")

                val dup = inBuffer.duplicate()
                dup.rewind()
                val inputTokenCount = dup.remaining() / 4
                var seed = 17L
                repeat(inputTokenCount) { seed = seed * 31 + dup.int }

                writeLogitsFor(inputTokenCount, seed, outBuffer)
            }

            override fun close() {
                // No native resources to release for the mock.
            }

            override fun getInputIndex(name: String): Int = 0

            override fun getOutputIndex(name: String): Int = 0

            override fun runForMultipleInputsOutputs(inputs: Array<Any>, outputs: Map<Int, Any>) {
                // Fan the same deterministic fake-logit generation out to every declared
                // output buffer, using the first provided input as the seed source.
                val primaryInput = inputs.firstOrNull()
                    ?: throw IllegalArgumentException("Mock interpreter requires at least one input buffer")
                outputs.values.forEach { output -> run(primaryInput, output) }
            }
        }

        val config = LLMConfig(
            vocabSize = vocabSize,
            maxSeqLen = maxSequenceLength,
            numLayers = 28,
            numHeads = 32,
            headDim = 128
        )

        return LLMRuntime(mockInterpreter, tokenizer, sampler, safetyClassifier, config)
    }
}