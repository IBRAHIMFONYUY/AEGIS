package com.aegis.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SimpleReasoningEngine : ReasoningEngine {


    override suspend fun generateResponse(
        prompt: String,
        context: String?,
        metadata: Map<String, String>
    ): String {

        return "Based on my analysis, this interaction shows signs of potential risk. " +
                "Please verify the sender's identity and avoid sharing sensitive information."
    }



    override fun generateResponseStream(
        prompt: String,
        context: String?
    ): Flow<String> {

        return flowOf(
            "Based on my analysis..."
        )
    }



    override suspend fun summarizeConversation(
        history: List<String>,
        metadata: Map<String, String>
    ): String {

        return """
            Conversation Summary:

            ${history.joinToString("\n")}

            Analysis:
            The conversation was evaluated using heuristic reasoning.
            Users should verify unexpected requests and avoid sharing sensitive data.
        """.trimIndent()
    }




    override suspend fun analyzeConversation(
        history: List<String>,
        currentMessage: String,
        metadata: Map<String, String>
    ): String {


        val suspiciousKeywords = listOf(
            "urgent",
            "password",
            "money",
            "bank",
            "verify",
            "secret"
        )


        val text =
            (history + currentMessage)
                .joinToString(" ")
                .lowercase()



        val detected =
            suspiciousKeywords.filter {
                text.contains(it)
            }



        return if (detected.isNotEmpty()) {

            """
            Risk Level: SUSPICIOUS

            Detected indicators:
            ${detected.joinToString(", ")}

            Recommendation:
            Verify the sender before taking action.
            """.trimIndent()

        } else {

            """
            Risk Level: SAFE

            No obvious manipulation indicators detected.
            """.trimIndent()
        }
    }





    override suspend fun isModelLoaded(): Boolean =
        true




    override suspend fun loadModel(): Boolean =
        true




    override fun getModelInfo(): ModelInfo {

        return ModelInfo(
            name = "SimpleReasoning",
            type = "Heuristic",
            isLoaded = true,
            version = "1.0"
        )
    }
}