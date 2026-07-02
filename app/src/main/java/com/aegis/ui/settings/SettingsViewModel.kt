package com.aegis.ui.settings

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.SettingsRepository
import com.aegis.services.accessibility.AegisAccessibilityService
import com.aegis.services.foreground.AegisForegroundService
import com.aegis.services.notification.AegisNotificationListener
import com.aegis.services.vpn.AegisVpnService
import com.aegis.services.workmanager.ScanWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val guardianCore: GuardianCore,
    private val appContext: Context
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
            _isDarkMode.value = settingsRepository.getBoolean(
                SettingsRepository.KEY_DARK_MODE, false
            )
            _isRealTimeScanning.value = settingsRepository.getBoolean(
                SettingsRepository.KEY_REAL_TIME_SCANNING, true
            )
            _isBackgroundScanning.value = settingsRepository.getBoolean(
                SettingsRepository.KEY_BACKGROUND_SCANNING, true
            )
            _services.value = getServiceStates()
        }
    }

    private suspend fun getServiceStates(): List<ServiceState> {
        return listOf(
            ServiceState(
                name = "Accessibility Service",
                isEnabled = isAccessibilityServiceEnabled(),
                isAvailable = true,
                description = "Monitor screen content for threats in real-time"
            ),
            ServiceState(
                name = "Notification Listener",
                isEnabled = isNotificationListenerEnabled(),
                isAvailable = true,
                description = "Analyze notifications for scam messages"
            ),
            ServiceState(
                name = "VPN Service",
                isEnabled = isVpnServiceEnabled(),
                isAvailable = true,
                description = "Block malicious domains and phishing sites"
            ),
            ServiceState(
                name = "Foreground Service",
                isEnabled = true,
                isAvailable = true,
                description = "Continuous background protection"
            ),
            ServiceState(
                name = "Clipboard Monitor",
                isEnabled = settingsRepository.getBoolean(
                    SettingsRepository.KEY_CLIPBOARD_MONITOR_ENABLED, true
                ),
                isAvailable = true,
                description = "Detect sensitive data in clipboard"
            )
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        try {
            val enabledServices = Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(appContext.packageName + "/" +
                    AegisAccessibilityService::class.java.name)
        } catch (e: Exception) {
            return false
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        try {
            val enabledListeners = Settings.Secure.getString(
                appContext.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return enabledListeners.contains(appContext.packageName + "/" +
                    AegisNotificationListener::class.java.name)
        } catch (e: Exception) {
            return false
        }
    }

    private fun isVpnServiceEnabled(): Boolean {
        return VpnService.prepare(appContext) == null
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        appContext.startActivity(intent)
    }

    fun openNotificationListenerSettings() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        appContext.startActivity(intent)
    }

    fun openVpnSettings() {
        val intent = VpnService.prepare(appContext) ?: return
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        appContext.startActivity(intent)
    }

    fun startForegroundService() {
        val intent = Intent(appContext, AegisForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    fun scheduleBackgroundScan() {
        ScanWorker.schedule(appContext)
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        viewModelScope.launch {
            settingsRepository.setBoolean(SettingsRepository.KEY_DARK_MODE, enabled)
        }
    }

    fun toggleRealTimeScanning(enabled: Boolean) {
        _isRealTimeScanning.value = enabled
        viewModelScope.launch {
            settingsRepository.setBoolean(SettingsRepository.KEY_REAL_TIME_SCANNING, enabled)
        }
    }

    fun toggleBackgroundScanning(enabled: Boolean) {
        _isBackgroundScanning.value = enabled
        viewModelScope.launch {
            settingsRepository.setBoolean(SettingsRepository.KEY_BACKGROUND_SCANNING, enabled)
        }
    }

    fun toggleClipboardMonitor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBoolean(SettingsRepository.KEY_CLIPBOARD_MONITOR_ENABLED, enabled)
            refreshServiceStates()
        }
    }

    fun refreshServiceStates() {
        viewModelScope.launch {
            _services.value = getServiceStates()
        }
    }
}
