package com.aegis.ai

import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer

/**
 * Abstraction over anything that can run token-in/logits-out inference the way
 * LLMRuntime needs. LiteRTInterpreter (the real TFLite-backed implementation below)
 * is final with a private constructor by design — it should only ever be built
 * through its factory methods, which is the right encapsulation for production use.
 * But that also means it can't be subclassed for tests. This interface exists so a
 * mock/fake implementation can stand in for it wherever LLMRuntime needs an
 * interpreter, without touching the real class's guarantees.
 */
interface LiteRTInterpreterOps {
    fun run(input: Any, output: Any)
    fun runForMultipleInputsOutputs(inputs: Array<Any>, outputs: Map<Int, Any>)
    fun getInputIndex(name: String): Int
    fun getOutputIndex(name: String): Int
    fun close()
}

/**
 * Wrapper for the LiteRT (formerly TFLite) Interpreter to support Gemma 3N and other models.
 */
class LiteRTInterpreter private constructor(private val interpreter: Interpreter) : LiteRTInterpreterOps {

    class Options {
        private var numThreads: Int = 4
        private var useNNAPI: Boolean = false
        private var useGPU: Boolean = false

        fun setNumThreads(threads: Int): Options {
            this.numThreads = threads
            return this
        }

        fun setUseNNAPI(useNNAPI: Boolean): Options {
            this.useNNAPI = useNNAPI
            return this
        }

        fun setUseGPU(useGPU: Boolean): Options {
            this.useGPU = useGPU
            return this
        }

        fun toTFLiteOptions(): Interpreter.Options {
            return Interpreter.Options().apply {
                setNumThreads(numThreads)
                setUseNNAPI(useNNAPI)
                // Add more options as needed
            }
        }
    }

    companion object {
        fun createFromFile(modelPath: String, options: Options): LiteRTInterpreter {
            val file = File(modelPath)
            val interpreter = Interpreter(file, options.toTFLiteOptions())
            return LiteRTInterpreter(interpreter)
        }

        fun createFromBuffer(modelBuffer: ByteBuffer, options: Options): LiteRTInterpreter {
            val interpreter = Interpreter(modelBuffer, options.toTFLiteOptions())
            return LiteRTInterpreter(interpreter)
        }
    }

    override fun run(input: Any, output: Any) {
        interpreter.run(input, output)
    }

    override fun runForMultipleInputsOutputs(inputs: Array<Any>, outputs: Map<Int, Any>) {
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
    }

    override fun getInputIndex(name: String): Int = interpreter.getInputIndex(name)

    override fun getOutputIndex(name: String): Int = interpreter.getOutputIndex(name)

    override fun close() {
        interpreter.close()
    }
}