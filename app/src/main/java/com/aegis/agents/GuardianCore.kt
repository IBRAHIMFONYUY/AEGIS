package com.aegis.agents

import com.aegis.ai.GemmaInferenceEngine
import com.aegis.core.*
import com.aegis.data.repository.GuardianMemoryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow

class GuardianCore(
    private val agents: List<GuardianAgent>,
    val memoryRepository: GuardianMemoryRepository? = null,
    val gemmaEngine: com.aegis.ai.GemmaInferenceEngine? = null,
    val analyticsManager: com.aegis.ai.analytics.SecurityAnalyticsManager? = null
) {

    private val engine = GuardianEngine(agents, memoryRepository, gemmaEngine)

    private val _analysisResults = MutableSharedFlow<AnalysisResult>(extraBufferCapacity = 64)
    val analysisResults = _analysisResults.asSharedFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _lastAnalysis = MutableStateFlow<AnalysisResult?>(null)
    val lastAnalysis = _lastAnalysis.asStateFlow()

    private val _guardianScore =
        MutableStateFlow(GuardianScore(1f, 1f, 1f, 1f, 1f, ScoreTrend.STABLE))

    val guardianScore: StateFlow<GuardianScore> = _guardianScore.asStateFlow()

    suspend fun analyze(context: AnalysisContext): AnalysisResult {
        _isAnalyzing.value = true
        try {

            val chatId = context.metadata["chat_id"] ?: context.sourceApp

            val enrichedContext = if (chatId != null) {
                val history = if (context.conversationHistory.isEmpty()) {
                    memoryRepository?.getConversationHistory(chatId) ?: emptyList()
                } else {
                    context.conversationHistory
                }
                
                val contactSummary = memoryRepository?.getContactSummary(chatId)
                val newMetadata = if (contactSummary != null) {
                    context.metadata + mapOf("contact_summary" to contactSummary)
                } else {
                    context.metadata
                }
                
                context.copy(conversationHistory = history, metadata = newMetadata)
            } else {
                context
            }

            // --- QUOTA PROTECTION: PRE-COMPUTE AI ANALYSIS ONCE ---
            // If this is a high-priority request (Chatbot or User-Triggered), 
            // we do ONE deep AI analysis and share it with all agents.
            val finalContext = if (enrichedContext.text != null && 
                (enrichedContext.metadata["source_type"] == "CHATBOT" || 
                 enrichedContext.metadata["force_ai_analysis"] == "true")) {
                
                val aiResult = gemmaEngine?.detectThreatWithAI(enrichedContext.text, enrichedContext.metadata)
                enrichedContext.copy(precomputedAiResult = aiResult)
            } else {
                enrichedContext
            }

            if (chatId != null && context.text != null) {
                memoryRepository?.storeConversationMessage(
                    chatId = chatId,
                    sender = context.metadata["sender"] ?: "Unknown",
                    message = context.text
                )
            }

            val result = engine.analyze(finalContext)

            _lastAnalysis.value = result
            _analysisResults.tryEmit(result)
            
            // Record for analytics
            analyticsManager?.recordAnalysis(result)

            _guardianScore.value = getDetailedGuardianScore()

            return result

        } finally {
            _isAnalyzing.value = false
        }
    }

    suspend fun analyzeAll(text: String, sourceApp: String? = null): List<AgentResult> {
        val context = AnalysisContext(text = text, sourceApp = sourceApp)
        return engine.analyze(context).agentResults
    }

    fun getDetailedGuardianScore(): GuardianScore {

        val history = engine.analysisHistory
        if (history.isEmpty()) {
            return GuardianScore(1f, 1f, 1f, 1f, 1f, ScoreTrend.STABLE)
        }

        val recent = history.takeLast(100)

        val privacy = score(recent, listOf("PrivacyAgent"))
        val scam = score(recent, listOf("ScamAgent"))
        val behavior = score(recent, listOf("IntentAgent"))
        val content = score(recent, listOf("CyberbullyingAgent", "MisinformationAgent"))
        val malware = score(recent, listOf("MalwareGuardianAgent"))

        val deviceSecurity = ((1f - malware) * 0.7f + (1f - behavior) * 0.3f)
            .coerceIn(0f, 1f)

        val overall =
            (privacy * 0.25f + scam * 0.35f + deviceSecurity * 0.25f + content * 0.15f)
                .coerceIn(0f, 1f)

        val trend = calculateTrend(recent, overall)

        return GuardianScore(
            overall = overall,
            privacy = privacy,
            scamProtection = scam,
            deviceSecurity = deviceSecurity,
            digitalWellbeing = content,
            trend = trend
        )
    }

    private fun calculateTrend(
        history: List<AnalysisResult>,
        currentScore: Float
    ): ScoreTrend {

        if (history.size < 10) return ScoreTrend.STABLE

        val previous = history.takeLast(10).map {
            val avgThreat = it.agentResults.map { r -> r.threatLevel.value }.average().toFloat()
            (1f - avgThreat).coerceIn(0f, 1f)
        }

        val avgPrevious = previous.average().toFloat()
        val diff = currentScore - avgPrevious

        return when {
            diff > 0.05f -> ScoreTrend.IMPROVING
            diff < -0.05f -> ScoreTrend.DECLINING
            else -> ScoreTrend.STABLE
        }
    }

    private fun score(history: List<AnalysisResult>, agents: List<String>): Float {
        val results = history
            .flatMap { it.agentResults }
            .filter { it.agentName in agents }

        if (results.isEmpty()) return 1.0f // Default to perfect score if no data

        val avgThreat = results.sumOf { it.threatLevel.value.toDouble() } / results.size
        // Normalize 0..4 to 1..0 (where 0 threat = 1.0 score, 4 threat = 0.0 score)
        return (1.0 - (avgThreat / 4.0)).toFloat().coerceIn(0f, 1f)
    }

    fun getOverallSafetyScore(): Float =
        getDetailedGuardianScore().overall

    fun getAgentStatuses(): List<AgentStatus> =
        agents.map {
            AgentStatus(
                name = it.name,
                version = it.version,
                isAvailable = it.isAvailable(),
                description = it.description
            )
        }

    fun getRecentThreats(limit: Int = 10): List<AnalysisResult> =
        engine.analysisHistory
            .filter { it.overallThreatLevel.value >= ThreatLevel.SUSPICIOUS.value }
            .takeLast(limit)
            .reversed()

    fun getAnalysisHistory(): List<AnalysisResult> =
        engine.analysisHistory.toList()

    val agentCount: Int get() = agents.size
    val availableAgentCount: Int get() = agents.count { it.isAvailable() }
    val engineInstance: GuardianEngine get() = engine
}

data class AgentStatus(
    val name: String,
    val version: String,
    val isAvailable: Boolean,
    val description: String
)