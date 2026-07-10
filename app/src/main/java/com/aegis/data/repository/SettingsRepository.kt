package com.aegis.data.repository

import com.aegis.data.db.dao.SettingsDao
import com.aegis.data.db.entity.AppSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val settingsDao: SettingsDao) {

    suspend fun getString(key: String, default: String = ""): String {
        return settingsDao.getSetting(key)?.value ?: default
    }

    suspend fun getInt(key: String, default: Int = 0): Int {
        return settingsDao.getSetting(key)?.value?.toIntOrNull() ?: default
    }

    suspend fun getBoolean(key: String, default: Boolean = false): Boolean {
        return settingsDao.getSetting(key)?.value?.toBooleanStrictOrNull() ?: default
    }

    suspend fun setString(key: String, value: String) {
        settingsDao.setSetting(AppSettings(key, value))
    }

    suspend fun setInt(key: String, value: Int) {
        settingsDao.setSetting(AppSettings(key, value.toString()))
    }

    suspend fun setBoolean(key: String, value: Boolean) {
        settingsDao.setSetting(AppSettings(key, value.toString()))
    }

    fun observeString(key: String): Flow<AppSettings?> = settingsDao.observeSetting(key)

    fun getAllSettings(): Flow<List<AppSettings>> = settingsDao.getAllSettings()

    suspend fun deleteSetting(key: String) {
        settingsDao.deleteSetting(key)
    }

    companion object {
        const val KEY_ACCESSIBILITY_ENABLED = "accessibility_enabled"
        const val KEY_NOTIFICATION_LISTENER_ENABLED = "notification_listener_enabled"
        const val KEY_VPN_ENABLED = "vpn_enabled"
        const val KEY_CLIPBOARD_MONITOR_ENABLED = "clipboard_monitor_enabled"
        const val KEY_REAL_TIME_SCANNING = "real_time_scanning"
        const val KEY_BACKGROUND_SCANNING = "background_scanning"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_SAFETY_SCORE = "safety_score"
        const val KEY_USER_NAME = "user_name"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        const val KEY_PRIVACY_MODE = "privacy_mode"
    }
}
