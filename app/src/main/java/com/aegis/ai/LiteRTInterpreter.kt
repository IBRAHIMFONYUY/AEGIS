package com.aegis.ai

import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer

/**
 * Wrapper for the LiteRT (formerly TFLite) Interpreter to support Gemma 3N and other models.
 */
class LiteRTInterpreter private constructor(private val interpreter: Interpreter) {

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

    fun run(input: Any, output: Any) {
        interpreter.run(input, output)
    }

    fun runForMultipleInputsOutputs(inputs: Array<Any>, outputs: Map<Int, Any>) {
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
    }

    fun getInputIndex(name: String): Int = interpreter.getInputIndex(name)
    
    fun getOutputIndex(name: String): Int = interpreter.getOutputIndex(name)

    fun close() {
        interpreter.close()
    }
}
