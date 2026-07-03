package com.aegis.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val guardianCore: GuardianCore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class ServiceState(
        val name: String,
        val isEnabled: Boolean,
        val isAvailable: Boolean,
        val description: String
    )

    private val _services = MutableStateFlow<List<ServiceState>>(emptyList())
    val services: StateFlow<List<ServiceState>> = _services.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isRealTimeScanning = MutableStateFlow(true)
    val isRealTimeScanning: StateFlow<Boolean> = _isRealTimeScanning.asStateFlow()

    private val _isBackgroundScanning = MutableStateFlow(true)
    val isBackgroundScanning: StateFlow<Boolean> = _isBackgroundScanning.asStateFlow()

    private val _agentVersions = MutableStateFlow(guardianCore.getAgentStatuses())
    val agentVersions = _agentVersions.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _isDarkMode.value = settingsRepository.getBoolean(SettingsRepository.KEY_DARK_MODE)
            _isRealTimeScanning.value = settingsRepository.getBoolean(SettingsRepository.KEY_REAL_TIME_SCANNING, true)
            _isBackgroundScanning.value = settingsRepository.getBoolean(SettingsRepository.KEY_BACKGROUND_SCANNING, true)
            updateServiceStates()
        }
    }

    private fun updateServiceStates() {
        val agentStatuses = guardianCore.getAgentStatuses()
        _services.value = agentStatuses.map { 
            ServiceState(it.name, it.isAvailable, it.isAvailable, it.description)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBoolean(SettingsRepository.KEY_DARK_MODE, enabled)
            _isDarkMode.value = enabled
        }
    }

    fun toggleRealTimeScanning(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBoolean(SettingsRepository.KEY_REAL_TIME_SCANNING, enabled)
            _isRealTimeScanning.value = enabled
        }
    }

    fun toggleBackgroundScanning(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBoolean(SettingsRepository.KEY_BACKGROUND_SCANNING, enabled)
            _isBackgroundScanning.value = enabled
        }
    }
}
