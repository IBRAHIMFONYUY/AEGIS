package com.aegis.ai

import android.content.Context
import android.util.Log
import com.aegis.ai.runtime.LLMRuntime
import com.aegis.ai.sampling.SamplingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class GemmaInferenceEngine(
    private val context: Context
) : InferenceEngine, ReasoningEngine {


    private val TAG = "GemmaInferenceEngine"

    private val gemmaManager =
        GemmaModelManager(context)


    val isModelLoadedFlow: StateFlow<Boolean>
        get() = gemmaManager.isModelLoaded


    val isLoading: StateFlow<Boolean>
        get() = gemmaManager.isLoading


    val loadProgress: StateFlow<Float>
        get() = gemmaManager.loadProgress


    val errorMessage: StateFlow<String?>
        get() = gemmaManager.errorMessage



    override suspend fun generateResponse(
        prompt: String,
        context: String?
    ): String {


        if (!ensureReady()) {
            return "Model not ready"
        }


        val formattedPrompt =
            formatPrompt(prompt, context)



        return when(
            val result =
                gemmaManager.generate(formattedPrompt)
        ){

            is GenerationResult.Text ->
                result.text


            is GenerationResult.Blocked ->
                "[Blocked] ${result.reason}"
        }
    }



    override fun generateResponseStream(
        prompt: String,
        context: String?
    ): Flow<String> {


        val formattedPrompt =
            formatPrompt(prompt, context)



        return gemmaManager
            .generateStream(formattedPrompt)

    }



    private fun formatPrompt(
        prompt:String,
        context:String?
    ):String {


        val ctx =
            if(!context.isNullOrBlank())
                "Context:\n$context\n\n"
            else
                ""


        return """
        <start_of_turn>user
        $ctx
        $prompt
        <end_of_turn>
        <start_of_turn>model
        
        """.trimIndent()
    }




    override suspend fun loadModel():Boolean {

        return gemmaManager.initializeEngine()

    }



    override suspend fun ensureReady():Boolean {


        if(gemmaManager.isModelLoaded.value)
            return true


        return loadModel()
    }



    override suspend fun isModelLoaded():Boolean {

        return gemmaManager.isModelLoaded.value

    }



    override fun getModelInfo():ModelInfo {


        val info =
            gemmaManager.getModelInfo()


        return ModelInfo(
            name = info.name,
            type = "Gemma 3N LiteRT-LM",
            isLoaded = info.isLoaded,
            version = info.version,
            sizeBytes = info.size
        )
    }





    override suspend fun classify(
        text:String,
        modelType:String
    ):Float {


        val prompt =
            """
            Classify this text as $modelType risk.
            
            Return ONLY a number between 0 and 1.
            
            Text:
            $text
            """.trimIndent()



        val result =
            generateResponse(prompt)



        return result
            .filter {
                it.isDigit() || it=='.'
            }
            .toFloatOrNull()
            ?:0.5f
    }





    override suspend fun analyzeText(
        text:String,
        modelType:String
    ):Map<String,Float>{


        val score =
            classify(text,modelType)


        return mapOf(
            "score" to score,
            "confidence" to 0.85f
        )
    }





    override suspend fun isModelLoaded(
        modelType:String
    ):Boolean =
        isModelLoaded()



    override suspend fun loadModel(
        modelType:String
    ):Boolean =
        loadModel()



    override fun getAvailableModels():List<ModelInfo> =
        listOf(getModelInfo())





    suspend fun detectScam(
        text:String,
        metadata:Map<String,String>
    ):Float =
        classify(text,"scam")





    suspend fun analyzeNotification(
        title:String,
        message:String,
        appName:String
    ):String {


        return generateResponse(
            """
            Analyze this notification.
            
            App:
            $appName
            
            Title:
            $title
            
            Message:
            $message
            
            Determine if it is suspicious.
            """.trimIndent()
        )
    }





    fun installModel():Flow<DownloadStatus> =
        gemmaManager.installModel()



    fun unloadModel(){

        gemmaManager.unloadModel()

    }



    fun close(){

        unloadModel()

    }


    override suspend fun summarizeConversation(
        history: List<String>
    ): String {

        val prompt = """
        Analyze this conversation history.

        Identify:
        - suspicious behavior
        - manipulation
        - scams
        - harassment
        - threats

        Conversation:

        ${history.joinToString("\n")}

    """.trimIndent()


        return generateResponse(prompt)
    }



    override suspend fun analyzeConversation(
        history: List<String>,
        currentMessage: String
    ): String {

        val prompt = """
        You are AEGIS Intent Analysis Engine.

        Analyze:

        History:
        ${history.joinToString("\n")}


        Current:
        $currentMessage


        Return:
        - Risk level
        - Attack technique
        - Explanation
        - Recommended action

    """.trimIndent()


        return generateResponse(prompt)
    }

}
