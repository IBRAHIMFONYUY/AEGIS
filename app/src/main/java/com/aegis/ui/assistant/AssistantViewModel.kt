package com.aegis.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val guardianCore: GuardianCore
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
            
            // Simulate reasoning/thinking phase
            val thinkingMsg = ChatMessage(text = "Analyzing intent...", isUser = false, isThinking = true)
            _messages.value = _messages.value + thinkingMsg
            
            val response = generateIntelligentResponse(text)
            
            _messages.value = _messages.value.filter { !it.isThinking }
            val botMessage = ChatMessage(text = response, isUser = false)
            _messages.value = _messages.value + botMessage
            _isTyping.value = false
        }
    }

    private suspend fun generateIntelligentResponse(query: String): String {
        // Use all agents to analyze the user's own message (Meta-analysis)
        val analysis = guardianCore.engineInstance.analyze(AnalysisContext(text = query))
        
        delay(1500) // Simulate processing

        val threats = analysis.agentResults.filter { it.threatLevel.value >= ThreatLevel.SUSPICIOUS.value }
        
        if (threats.isNotEmpty()) {
            return buildString {
                appendLine("🔍 **Guardian Meta-Analysis Result**")
                appendLine("I've detected potentially risky intent in your own query:")
                threats.forEach { appendLine("• ${it.reason}") }
                appendLine("\nAre you asking because you encountered this elsewhere? I recommend caution.")
            }
        }

        // Context-aware conversational logic
        val queryLower = query.lowercase()
        val score = guardianCore.getOverallSafetyScore()
        
        return when {
            queryLower.contains("status") || queryLower.contains("how am i") -> {
                "Your current Safety Score is ${(score * 100).toInt()}%. " + 
                if (score > 0.8) "You're doing great! No major threats detected recently." 
                else "I've flagged some suspicious activities. Check your Threat Log for details."
            }
            queryLower.contains("who are you") || queryLower.contains("what is aegis") -> {
                "I am AEGIS—an Autonomous Ethical Guardian Intelligent System. Unlike standard AI, I live entirely on your device to protect your privacy while monitoring for scams, intent-based manipulation, and data leaks in real-time."
            }
            queryLower.contains("scam") || queryLower.contains("phishing") -> {
                "I use specialized agents to detect scams. For example, if a message uses 'Coercion' (creating fake urgency) or 'Authority Impersonation', I'll alert you immediately with an overlay."
            }
            queryLower.contains("real") || queryLower.contains("fake") -> {
                "My intelligence is derived from local neural networks (LiteRT/ONNX). While I don't use cloud-based LLMs to keep your data 100% private, I can reason about the ethical and security implications of any text you show me."
            }
            queryLower.contains("help") -> {
                "I can explain your recent threats, help you improve your safety score, or analyze any text you paste here for hidden malicious intent."
            }
            else -> {
                "I've processed your request using my ${guardianCore.availableAgentCount} active agents. " +
                "Is there a specific message or app behavior you want me to analyze for you?"
            }
        }
    }

    fun getQuickReplies(): List<String> = listOf(
        "Check my safety status",
        "Explain how you protect me",
        "Analyze recent threats",
        "How does local AI work?"
    )
}
