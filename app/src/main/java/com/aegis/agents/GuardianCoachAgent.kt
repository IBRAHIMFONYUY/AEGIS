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
            // The Coach agent typically doesn't detect threats itself but explains them.
            // However, it can analyze the context to provide preliminary advice.
            
            val prompt = buildExplanationPrompt(context, previousResults)
            val explanation = reasoningEngine?.generateResponse(prompt) ?: "I am analyzing this interaction to ensure your safety."

            AgentResult(
                agentName = name,
                threatLevel = ThreatLevel.SAFE, // The coach itself isn't a threat detector
                confidence = 1.0f,
                reason = explanation,
                suggestedAction = "Follow the guidance provided to stay safe."
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
