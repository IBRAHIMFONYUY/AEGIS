package com.aegis.agents

import com.aegis.ai.ImageAnalyzer
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageGuardianAgent(
    private val imageAnalyzer: ImageAnalyzer,
    private val agents: List<GuardianAgent>
) : GuardianAgent {

    override val name = "ImageGuardian"
    override val version = "1.0.0"
    override val description = "Analyzes screenshots and images for hidden scams and sensitive data"

    override suspend fun analyze(
        context: AnalysisContext,
        memory: GuardianMemoryRepository?,
        previousResults: List<AgentResult>
    ): AgentResult = withContext(Dispatchers.Default) {
        val imagePath = context.imagePath ?: return@withContext safeResult
        
        val extractedText = imageAnalyzer.extractTextFromImage(imagePath)
            ?: return@withContext safeResult.copy(reason = "No text found in image")
        
        // Delegate analysis of extracted text to other agents
        val textContext = context.copy(
            text = extractedText,
            sourceType = SourceType.IMAGE,
            metadata = context.metadata + mapOf("is_ocr_extracted" to "true")
        )
        
        val results = agents
            .filter { it.name != name && it.name != "GuardianCoach" }
            .map { it.analyze(textContext, memory, emptyList()) }
        
        val maxThreatResult = results.maxByOrNull { it.threatLevel.value }
        
        if (maxThreatResult == null || maxThreatResult.threatLevel == ThreatLevel.SAFE) {
            return@withContext safeResult.copy(
                reason = "Image scan complete. No threats detected.",
                details = mapOf("ocr_text_length" to extractedText.length.toString())
            )
        }
        
        AgentResult(
            agentName = name,
            threatLevel = maxThreatResult.threatLevel,
            confidence = maxThreatResult.confidence,
            reason = "⚠ Security threat detected in image: ${maxThreatResult.reason}",
            details = maxThreatResult.details + mapOf(
                "ocr_text" to extractedText.take(100),
                "original_agent" to maxThreatResult.agentName
            ),
            suggestedAction = maxThreatResult.suggestedAction,
            requiresUserAttention = maxThreatResult.requiresUserAttention
        )
    }

    private val safeResult get() = AgentResult(
        agentName = name,
        threatLevel = ThreatLevel.SAFE,
        confidence = 0f,
        reason = "No image to analyze"
    )
}
