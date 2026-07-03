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
    private val reasoningEngine: ReasoningEngine
) : ViewModel() {

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val isThinking: Boolean = false
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! I am AEGIS, your Autonomous Ethical Guardian. I've been monitoring your device for threats. How can I assist your digital safety today?",
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
            
            // Collect context from recent threats for the AI
            val recentThreats = guardianCore.getRecentThreats(5)
            val context = if (recentThreats.isNotEmpty()) {
                "Recent security events detected:\n" + 
                recentThreats.joinToString("\n") { "- ${it.overallThreatLevel.label}: ${it.agentResults.firstOrNull()?.reason}" }
            } else {
                "No recent threats detected. Device is safe."
            }

            val response = try {
                reasoningEngine.generateResponse(text, context)
            } catch (e: Exception) {
                "I apologize, I'm having trouble processing that right now. Please ensure your local guardian engine is active."
            }

            _messages.value = _messages.value + ChatMessage(text = response, isUser = false)
            _isTyping.value = false
        }
    }
}
