package com.aegis.ui.threatlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.ai.ReasoningEngine
import com.aegis.data.db.entity.ThreatEvent
import com.aegis.data.repository.ThreatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThreatDetailViewModel @Inject constructor(
    private val threatRepository: ThreatRepository,
    private val guardianCore: GuardianCore,
    private val reasoningEngine: ReasoningEngine
) : ViewModel() {

    private val _threat = MutableStateFlow<ThreatEvent?>(null)
    val threat: StateFlow<ThreatEvent?> = _threat.asStateFlow()

    private val _aiAnalysis = MutableStateFlow<String?>(null)
    val aiAnalysis: StateFlow<String?> = _aiAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    fun loadThreat(id: Long) {
        viewModelScope.launch {
            _threat.value = threatRepository.getThreatById(id)
        }
    }

    fun analyzeWithGemini() {
        val currentThreat = _threat.value ?: return
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val context = "Threat Type: ${currentThreat.agentName}\n" +
                        "Source App: ${currentThreat.sourceApp}\n" +
                        "Message: ${currentThreat.sourceText}\n" +
                        "Initial Reason: ${currentThreat.reason}"
                
                val prompt = "Perform a deep security audit on this message. Explain why it is a scam, " +
                        "the psychological tactics used, and provide 3 specific steps for the user to stay safe."
                
                val metadata = mapOf("force_ai_analysis" to "true", "source_type" to "CHATBOT")
                val response = reasoningEngine.generateResponse(prompt, context, metadata)
                _aiAnalysis.value = response
            } catch (e: Exception) {
                _aiAnalysis.value = "AI Audit failed: ${e.localizedMessage}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
}
