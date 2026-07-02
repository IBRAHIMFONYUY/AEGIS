package com.aegis.core

data class AgentResult(
    val agentName: String,
    val threatLevel: ThreatLevel,
    val confidence: Float,
    val reason: String,
    val details: Map<String, String> = emptyMap(),
    val suggestedAction: String? = null,
    val requiresUserAttention: Boolean = false
)
