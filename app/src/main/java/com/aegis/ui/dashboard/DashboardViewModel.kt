package com.aegis.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.ThreatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val guardianCore: GuardianCore,
    private val safetyRepository: SafetyRepository,
    private val threatRepository: ThreatRepository
) : ViewModel() {

    val safetyScore: StateFlow<com.aegis.data.db.entity.SafetyScore?> = safetyRepository.getLatestScore()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
