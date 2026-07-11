package com.aegis.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.core.AnalysisResult
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
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        /** Single source of truth for the "protected" threshold used by the dashboard UI. */
        const val SAFE_THRESHOLD = 0.8f
        private const val AGENT_STATUS_POLL_INTERVAL_MS = 30_000L
    }

    val userName: StateFlow<String> = flow {
        emit(settingsRepository.getString(SettingsRepository.KEY_USER_NAME, "Guardian"))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Guardian")

    val safetyScore: StateFlow<com.aegis.data.db.entity.SafetyScore?> = safetyRepository.getLatestScore()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentThreats: StateFlow<List<com.aegis.data.db.entity.ThreatEvent>> = threatRepository.getRecentThreats(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAnalyzing: StateFlow<Boolean> = guardianCore.isAnalyzing

    val lastResult: StateFlow<AnalysisResult?> = guardianCore.lastAnalysis

    /**
     * Derived "is the device currently protected" flag, computed once here instead of the
     * UI re-deriving `guardianScore.overall >= SAFE_THRESHOLD` itself. Keeps the threshold
     * and the comparison in one testable place.
     */
    val isProtected: StateFlow<Boolean> = guardianCore.guardianScore
        .map { it.overall >= SAFE_THRESHOLD }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /**
     * Time-of-day greeting, exposed as a flow (rather than computed once in the composable)
     * so it stays correct if the dashboard is left open across an hour boundary or the
     * process is restored from a long-lived saved instance.
     */
    val greeting: StateFlow<String> = flow {
        while (true) {
            emit(currentGreeting())
            // Recompute at the next hour boundary rather than polling constantly.
            val now = Calendar.getInstance()
            val minutesToNextHour = 60 - now.get(Calendar.MINUTE)
            delay(minutesToNextHour * 60_000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), currentGreeting())

    val protectionStats: StateFlow<ProtectionSummary> = combine(
        threatRepository.getScamsBlockedCount(getStartOfDay()),
        threatRepository.getLinksBlockedCount(getStartOfDay()),
        threatRepository.getFakeNewsDetectedCount(getStartOfDay()),
        guardianCore.analyticsManager?.analytics ?: MutableStateFlow(com.aegis.ai.analytics.SecurityAnalyticsManager.SecurityStats())
    ) { scams, links, fakeNews, analytics ->
        ProtectionSummary(
            scamsBlocked = scams,
            linksBlocked = links,
            fakeNewsDetected = fakeNews,
            micSecured = hasPermission(Manifest.permission.RECORD_AUDIO),
            cameraSecured = hasPermission(Manifest.permission.CAMERA),
            totalAnalyzed = analytics.totalMessagesAnalyzed,
            avgConfidence = analytics.averageConfidence
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProtectionSummary())

    private val _agentStatuses = MutableStateFlow(guardianCore.getAgentStatuses())
    val agentStatuses = _agentStatuses.asStateFlow()

    /**
     * Surfaces scan failures to the UI (e.g. as a Snackbar) instead of the previous
     * behavior where a thrown exception in runFullScan() silently ended the coroutine
     * with no user-visible signal. Call [consumeScanError] after showing it once.
     */
    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    init {
        viewModelScope.launch {
            guardianCore.analysisResults.collect {
                refreshData()
            }
        }

        viewModelScope.launch {
            while (true) {
                ensureActive()
                try {
                    _agentStatuses.value = guardianCore.getAgentStatuses()
                } catch (e: Exception) {
                    // Don't let a single failed poll kill the loop or freeze the UI silently.
                    // Wire this into your crash/telemetry reporter.
                }
                delay(AGENT_STATUS_POLL_INTERVAL_MS)
            }
        }
    }

    private fun currentGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
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
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun refreshData() {
        _agentStatuses.value = guardianCore.getAgentStatuses()
    }

    fun runFullScan() {
        viewModelScope.launch {
            try {
                val analysisContext = com.aegis.core.AnalysisContext(
                    text = "System wide scan initiated by user",
                    sourceType = com.aegis.core.SourceType.UNKNOWN,
                    metadata = mapOf("deep_scan" to "true")
                )
                guardianCore.analyze(analysisContext)
                _agentStatuses.value = guardianCore.getAgentStatuses()
            } catch (e: Exception) {
                _scanError.value = "Guardian scan couldn't complete. Please try again."
            }
        }
    }

    /** Call from the UI after a scan-error Snackbar/toast has been shown, to avoid re-showing it. */
    fun consumeScanError() {
        _scanError.value = null
    }
}

data class ProtectionSummary(
    val scamsBlocked: Int = 0,
    val linksBlocked: Int = 0,
    val fakeNewsDetected: Int = 0,
    val micSecured: Boolean = false,
    val cameraSecured: Boolean = false,
    val totalAnalyzed: Long = 0,
    val avgConfidence: Float = 0f
)