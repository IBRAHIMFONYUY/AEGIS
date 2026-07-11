package com.aegis.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.ai.ReasoningEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val guardianCore: GuardianCore,
    private val reasoningEngine: com.aegis.ai.ReasoningEngine
) : ViewModel() {

    val isModelLoaded: StateFlow<Boolean> = if (reasoningEngine is com.aegis.ai.GemmaInferenceEngine) {
        reasoningEngine.isModelLoadedFlow
    } else {
        MutableStateFlow(true).asStateFlow()
    }





    val loadProgress: StateFlow<Float> = if (reasoningEngine is com.aegis.ai.GemmaInferenceEngine) {
        reasoningEngine.loadProgress
    } else {
        MutableStateFlow(1f).asStateFlow()
    }

    val isLoading: StateFlow<Boolean> = if (reasoningEngine is com.aegis.ai.GemmaInferenceEngine) {
        reasoningEngine.isLoading
    } else {
        MutableStateFlow(false).asStateFlow()
    }

    val isOnlineMode: StateFlow<Boolean> = if (reasoningEngine is com.aegis.ai.GemmaInferenceEngine) {
        reasoningEngine.useOnlineMode
    } else {
        MutableStateFlow(true).asStateFlow()
    }

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _installError = MutableStateFlow<String?>(null)
    val installError = _installError.asStateFlow()

    fun triggerInstall() {
        if (reasoningEngine is com.aegis.ai.GemmaInferenceEngine) {
            viewModelScope.launch {
                reasoningEngine.installModel().collect { status ->
                    when (status) {
                        is com.aegis.ai.DownloadStatus.Progress -> {
                            _downloadProgress.value = status.percentage
                        }
                        is com.aegis.ai.DownloadStatus.Success -> {
                            _downloadProgress.value = 100
                            _installError.value = null
                            reasoningEngine.loadModel()
                        }
                        is com.aegis.ai.DownloadStatus.Error -> {
                            _installError.value = status.message
                            _downloadProgress.value = null
                        }
                    }
                }
            }
        }
    }

    fun triggerLoad() {
        if (reasoningEngine is com.aegis.ai.GemmaInferenceEngine) {
            viewModelScope.launch {
                reasoningEngine.loadModel()
            }
        }
    }

    fun triggerScan() {
        triggerLoad()
    }

    fun importModel(uri: android.net.Uri) {
        if (reasoningEngine is com.aegis.ai.GemmaInferenceEngine) {
            viewModelScope.launch {
                val success = reasoningEngine.importModel(uri)
                if (success) {
                    reasoningEngine.loadModel()
                } else {
                    _installError.value = "Failed to import model from file."
                }
            }
        }
    }

    fun togglePrivacyMode(enabled: Boolean) {
        if (reasoningEngine is com.aegis.ai.GemmaInferenceEngine) {
            reasoningEngine.setPrivacyMode(enabled)
        }
    }

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val isThinking: Boolean = false
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! I am your AEGIS AI Guardian. I monitor your digital world to keep you and your data safe. How can I help you make a safe decision today?",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isTyping.value = true
            
            // Collect deep context for the AI
            val score = guardianCore.getDetailedGuardianScore()
            val recentThreats = guardianCore.getRecentThreats(10)
            val analytics = guardianCore.analyticsManager?.analytics?.value
            
            val context = buildString {
                append("DEVICE SECURITY CONTEXT:\n")
                append("- Overall Safety Score: ${(score.overall * 100).toInt()}%\n")
                append("- Privacy Score: ${(score.privacy * 100).toInt()}%\n")
                append("- Scam Protection: ${(score.scamProtection * 100).toInt()}%\n")
                append("- Device Health: ${(score.deviceSecurity * 100).toInt()}%\n")
                
                if (analytics != null) {
                    append("\nANALYTICS:\n")
                    append("- Total Messages Analyzed: ${analytics.totalMessagesAnalyzed}\n")
                    append("- Threats Blocked: ${analytics.totalThreatsBlocked}\n")
                    append("- AI Confidence: ${(analytics.averageConfidence * 100).toInt()}%\n")
                }
                
                if (recentThreats.isNotEmpty()) {
                    append("\nRECENT DETECTED THREATS:\n")
                    recentThreats.forEach { threat ->
                        append("- [${threat.overallThreatLevel.label}] ${threat.context.sourceApp ?: "Unknown"}: ${threat.agentResults.firstOrNull()?.reason}\n")
                    }
                }
                
                append("\nCONVERSATION HISTORY:\n")
                _messages.value.takeLast(10).forEach { msg ->
                    append("${if (msg.isUser) "User" else "AI"}: ${msg.text}\n")
                }
            }

            val response = try {
                val metadata = mapOf("force_ai_analysis" to "true", "source_type" to "CHATBOT")
                reasoningEngine.generateResponse(text, context, metadata)
            } catch (e: Exception) {
                "I apologize, I'm having trouble processing that right now. Please ensure your local guardian engine is active."
            }

            _messages.value = _messages.value + ChatMessage(text = response, isUser = false)
            _isTyping.value = false
        }
    }

    fun getQuickReplies(): List<String> = listOf(
        "Is this WhatsApp message a scam?",
        "Can I trust this banking website?",
        "Why is this app requesting my microphone?",
        "Should I install this APK?",
        "Check my latest security alerts"
    )
}
