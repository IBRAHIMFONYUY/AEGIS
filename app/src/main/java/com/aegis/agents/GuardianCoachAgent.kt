package com.aegis.agents

import com.aegis.ai.ReasoningEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GuardianCoachAgent(
    private val reasoningEngine: ReasoningEngine? = null
) : GuardianAgent {

    override val name = "GuardianCoach"
    override val version = "2.0.0"
    override val description = "Provides human-readable explanations and educational advice for detected threats"

    override suspend fun analyze(
        context: AnalysisContext, 
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult =
        withContext(Dispatchers.Default) {
            val prompt = buildExplanationPrompt(context, previousResults)
            val explanation = reasoningEngine?.generateResponse(prompt) ?: "I am performing a deep AI analysis of this interaction."

            AgentResult(
                agentName = name,
                threatLevel = ThreatLevel.SAFE, 
                confidence = 1.0f,
                reason = explanation,
                suggestedAction = "Follow this AI-generated guidance to maintain your digital safety."
            )
        }

    private fun buildExplanationPrompt(context: AnalysisContext, previousResults: List<AgentResult>): String {
        val threatReport = previousResults
            .filter { it.threatLevel.value >= ThreatLevel.SUSPICIOUS.value }
            .joinToString("\n") { "- ${it.agentName}: ${it.reason}" }

        return """
            Analyze the following mobile interaction for security risks:
            Text: ${context.text ?: "N/A"}
            Source App: ${context.sourceApp ?: "Unknown"}
            Source Type: ${context.sourceType}
            Is Unknown Sender: ${context.isUnknownSender}
            
            Detected Threats:
            $threatReport
            
            Provide a concise, human-friendly, educational explanation of potential risks and clear, actionable advice.
            Explain WHY this is risky based on the detected threats.
        """.trimIndent()
    }

    override fun isAvailable(): Boolean = reasoningEngine != null
}
