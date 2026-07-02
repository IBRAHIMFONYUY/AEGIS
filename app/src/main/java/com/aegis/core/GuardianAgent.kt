package com.aegis.core

import com.aegis.data.repository.GuardianMemoryRepository

interface GuardianAgent {
    val name: String
    val version: String
    val description: String
    suspend fun analyze(
        context: AnalysisContext, 
        memory: GuardianMemoryRepository? = null,
        previousResults: List<AgentResult> = emptyList()
    ): AgentResult
    fun isAvailable(): Boolean = true
}
