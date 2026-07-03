package com.aegis.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.SettingsRepository
import com.aegis.data.repository.ThreatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val guardianCore: GuardianCore,
    private val safetyRepository: SafetyRepository,
    private val threatRepository: ThreatRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val userName: StateFlow<String> = flow {
        emit(settingsRepository.getString(SettingsRepository.KEY_USER_NAME, "Guardian"))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Guardian")

    val safetyScore: StateFlow<com.aegis.data.db.entity.SafetyScore?> = safetyRepository.getLatestScore()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentThreats: StateFlow<List<com.aegis.data.db.entity.ThreatEvent>> = threatRepository.getRecentThreats(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val protectionStats: StateFlow<ProtectionSummary> = combine(
        threatRepository.getScamsBlockedCount(getStartOfDay()),
        threatRepository.getLinksBlockedCount(getStartOfDay()),
        threatRepository.getFakeNewsDetectedCount(getStartOfDay())
    ) { scams, links, fakeNews ->
        ProtectionSummary(
            scamsBlocked = scams,
            linksBlocked = links,
            fakeNewsDetected = fakeNews,
            micSecured = hasPermission(Manifest.permission.RECORD_AUDIO),
            cameraSecured = hasPermission(Manifest.permission.CAMERA)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProtectionSummary())

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

    private fun getStartOfDay(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun refreshData() {
        _agentStatuses.value = guardianCore.getAgentStatuses()
    }
}

data class ProtectionSummary(
    val scamsBlocked: Int = 0,
    val linksBlocked: Int = 0,
    val fakeNewsDetected: Int = 0,
    val micSecured: Boolean = false,
    val cameraSecured: Boolean = false
)
