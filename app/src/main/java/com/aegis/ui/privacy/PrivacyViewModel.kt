package com.aegis.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.SettingsRepository
import com.aegis.core.GuardianScore
import com.aegis.core.ScoreTrend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val guardianCore: GuardianCore,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val privacyScore: StateFlow<Float> = flow<Float> {
        emit(guardianCore.getDetailedGuardianScore().privacy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    private val _isCameraProtected = MutableStateFlow(true)
    val isCameraProtected = _isCameraProtected.asStateFlow()

    private val _isMicProtected = MutableStateFlow(true)
    val isMicProtected = _isMicProtected.asStateFlow()

    init {
        viewModelScope.launch {
            _isCameraProtected.value = settingsRepository.getBoolean("camera_protection", true)
            _isMicProtected.value = settingsRepository.getBoolean("mic_protection", true)
        }
    }

    fun toggleCameraProtection(enabled: Boolean) {
        viewModelScope.launch {
            _isCameraProtected.value = enabled
            settingsRepository.setBoolean("camera_protection", enabled)
        }
    }

    fun toggleMicProtection(enabled: Boolean) {
        viewModelScope.launch {
            _isMicProtected.value = enabled
            settingsRepository.setBoolean("mic_protection", enabled)
        }
    }

    fun runDeepScan() {
        viewModelScope.launch {
            val context = com.aegis.core.AnalysisContext(
                text = "Privacy deep scan",
                sourceType = com.aegis.core.SourceType.SCREEN,
                metadata = mapOf("deep_scan" to "true")
            )
            guardianCore.analyze(context)
        }
    }
}
