package com.aegis.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.core.AnalysisResult
import com.aegis.core.ThreatLevel
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.ThreatRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DashboardViewModel(
    private val guardianCore: GuardianCore,
    private val safetyRepository: SafetyRepository,
    private val threatRepository: ThreatRepository
) : ViewModel() {

    val safetyScore: StateFlow<Float> = safetyRepository.getLatestScore()
        .map { it?.score ?: 1.0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val recentThreats: StateFlow<List<com.aegis.data.db.entity.ThreatEvent>> = threatRepository.getRecentThreats(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeThreatCount: StateFlow<Int> = threatRepository.getCriticalThreatCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalScans: StateFlow<Int> = threatRepository.getTotalThreatCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _agentStatuses = MutableStateFlow(guardianCore.getAgentStatuses())
    val agentStatuses = _agentStatuses.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                ensureActive()
                _agentStatuses.value = guardianCore.getAgentStatuses()
                delay(30000)
            }
        }
    }

    fun refreshData() {
        _agentStatuses.value = guardianCore.getAgentStatuses()
    }
}
