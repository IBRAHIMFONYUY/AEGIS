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
            // --- QUOTA PROTECTION: Use precomputed AI reason if available ---
            val explanation = if (context.precomputedAiResult != null) {
                context.precomputedAiResult.reason
            } else {
                val prompt = buildExplanationPrompt(context, previousResults)
                reasoningEngine?.generateResponse(prompt, null, context.metadata) ?: "I am performing a deep AI analysis of this interaction."
            }

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

        val chatHistory = if (context.conversationHistory.isNotEmpty()) {
            "\nRecent Chat History:\n" + context.conversationHistory.joinToString("\n")
        } else ""

        val contactSummary = context.metadata["contact_summary"]?.let {
            "\nHistorical Summary of this Contact:\n$it"
        } ?: ""

        val isCyberbullying = previousResults.any { it.agentName == "CyberbullyingAgent" && it.threatLevel.value >= ThreatLevel.MALICIOUS.value }

        val personaPrompt = if (isCyberbullying) {
            "You are AEGIS Cyber-Safety Coach. You are speaking to someone who might be experiencing online harassment or bullying. Be extremely empathetic, supportive, and calm. Do not judge. Focus on their well-being and provide clear steps for safety and emotional support."
        } else {
            "You are AEGIS Security Coach. Provide a concise, human-friendly, educational explanation of the detected security risks."
        }

        val sender = context.metadata["sender"] ?: "Unknown Sender"
        val sourceApp = context.metadata["source_app"] ?: context.sourceApp ?: "Unknown App"

        return """
            $personaPrompt
            
            Context Analysis:
            Text: ${context.text ?: "N/A"}
            Sender: $sender
            App: $sourceApp
            Source Type: ${context.sourceType}
            Is Unknown Sender: ${context.isUnknownSender}
            $chatHistory
            $contactSummary
            
            Detected Threats:
            $threatReport
            
            Goal:
            - Explain WHY this message from $sender in $sourceApp is risky in plain language.
            - If it's cyberbullying, offer emotional validation first.
            - Provide 3 clear, actionable safety steps.
            - If there is chat history, use it to explain WHY the current message is suspicious compared to previous ones.
        """.trimIndent()
    }

    override fun isAvailable(): Boolean = reasoningEngine != null
}
