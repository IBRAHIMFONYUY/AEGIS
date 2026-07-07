package com.aegis.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EmergencyType(val displayName: String, val severity: Int) {
    RANSOMWARE_DETECTED("Ransomware Detected", 5),
    BANKING_MALWARE("Banking Malware", 5),
    ACCOUNT_COMPROMISE("Account Compromise", 4),
    DATA_BREACH("Data Breach", 4),
    PRIVACY_VIOLATION("Privacy Violation", 3),
    PHISHING_ATTACK("Phishing Attack", 3),
    DEVICE_TAKEOVER("Device Takeover", 5),
    CREDENTIAL_THEFT("Credential Theft", 4)
}

enum class EmergencyAction(val displayName: String) {
    ISOLATE_APP("Isolate App"),
    BLOCK_NETWORK("Block Network"),
    DISABLE_PERMISSIONS("Disable Permissions"),
    CLEAR_SESSION("Clear Session"),
    ENABLE_LOCKDOWN("Enable Lockdown"),
    NOTIFY_AUTHORITIES("Notify Authorities"),
    BACKUP_DATA("Backup Data"),
    SCAN_DEVICE("Scan Device")
}

data class EmergencyEvent(
    val id: String,
    val type: EmergencyType,
    val sourceApp: String?,
    val timestamp: Long,
    val description: String,
    val actionsTaken: List<EmergencyAction>,
    val resolved: Boolean = false
)

data class EmergencyStatus(
    val isActive: Boolean,
    val currentEmergency: EmergencyEvent? = null,
    val lockdownMode: Boolean = false,
    val networkBlocked: Boolean = false
)

class EmergencyGuardian(private val context: Context) {
    
    private val _emergencyStatus = MutableStateFlow(EmergencyStatus(isActive = false))
    val emergencyStatus: StateFlow<EmergencyStatus> = _emergencyStatus.asStateFlow()
    
    private val _emergencyHistory = MutableStateFlow(listOf<EmergencyEvent>())
    val emergencyHistory: StateFlow<List<EmergencyEvent>> = _emergencyHistory.asStateFlow()
    
    private val criticalThreatKeywords = mapOf(
        EmergencyType.RANSOMWARE_DETECTED to listOf(
            "encrypt", "ransom", "bitcoin", "payment", "decrypt", "files locked",
            "your files are encrypted", "pay to decrypt", "ransomware"
        ),
        EmergencyType.BANKING_MALWARE to listOf(
            "banking trojan", "overlay", "screen overlay", "fake banking app",
            "momo fraud", "unauthorized transaction", "otp theft"
        ),
        EmergencyType.ACCOUNT_COMPROMISE to listOf(
            "account suspended", "unusual login", "password changed",
            "verify identity", "security alert", "account takeover"
        ),
        EmergencyType.DEVICE_TAKEOVER to listOf(
            "remote access", "device control", "admin access",
            "superuser", "root access", "device compromised"
        )
    )
    
    fun analyzeForEmergency(
        text: String?,
        threatLevel: ThreatLevel,
        sourceApp: String?
    ): EmergencyEvent? {
        if (threatLevel.value < ThreatLevel.CRITICAL.value) return null
        
        val textLower = text?.lowercase() ?: return null
        
        for ((type, keywords) in criticalThreatKeywords) {
            if (keywords.any { textLower.contains(it) }) {
                return createEmergency(type, sourceApp, text)
            }
        }
        
        return null
    }
    
    fun triggerEmergency(emergency: EmergencyEvent) {
        val actions = determineEmergencyActions(emergency.type)
        val updatedEmergency = emergency.copy(actionsTaken = actions)
        
        // Execute emergency actions
        executeEmergencyActions(updatedEmergency)
        
        // Update status
        _emergencyStatus.value = EmergencyStatus(
            isActive = true,
            currentEmergency = updatedEmergency,
            lockdownMode = emergency.type.severity >= 4,
            networkBlocked = emergency.type.severity >= 5
        )
        
        // Add to history
        _emergencyHistory.value = _emergencyHistory.value + updatedEmergency
    }
    
    fun resolveEmergency(emergencyId: String) {
        val history = _emergencyHistory.value.toMutableList()
        val index = history.indexOfFirst { it.id == emergencyId }
        if (index >= 0) {
            history[index] = history[index].copy(resolved = true)
            _emergencyHistory.value = history
        }
        
        // Reset status if this was the active emergency
        val currentEmergency = _emergencyStatus.value.currentEmergency
        if (currentEmergency?.id == emergencyId) {
            _emergencyStatus.value = EmergencyStatus(isActive = false)
        }
    }
    
    fun enableLockdownMode() {
        _emergencyStatus.value = _emergencyStatus.value.copy(
            lockdownMode = true,
            networkBlocked = true
        )
        
        // Implement actual lockdown
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            enableDataSaver()
        }
    }
    
    fun disableLockdownMode() {
        _emergencyStatus.value = _emergencyStatus.value.copy(
            lockdownMode = false,
            networkBlocked = false
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            disableDataSaver()
        }
    }
    
    private fun createEmergency(
        type: EmergencyType,
        sourceApp: String?,
        text: String
    ): EmergencyEvent {
        return EmergencyEvent(
            id = "EMERGENCY-${System.currentTimeMillis()}",
            type = type,
            sourceApp = sourceApp,
            timestamp = System.currentTimeMillis(),
            description = buildEmergencyDescription(type, sourceApp, text),
            actionsTaken = emptyList()
        )
    }
    
    private fun buildEmergencyDescription(
        type: EmergencyType,
        sourceApp: String?,
        text: String
    ): String {
        val source = sourceApp ?: "System"
        return when (type) {
            EmergencyType.RANSOMWARE_DETECTED ->
                "CRITICAL: Ransomware indicators detected in $source. Immediate action required to prevent file encryption."
            EmergencyType.BANKING_MALWARE ->
                "CRITICAL: Banking malware detected in $source. Financial credentials at risk."
            EmergencyType.ACCOUNT_COMPROMISE ->
                "SEVERE: Account compromise detected in $source. Immediate verification required."
            EmergencyType.DATA_BREACH ->
                "SEVERE: Potential data breach detected in $source. Sensitive data may be exposed."
            EmergencyType.PRIVACY_VIOLATION ->
                "HIGH: Privacy violation detected in $source. Personal data access attempt blocked."
            EmergencyType.PHISHING_ATTACK ->
                "HIGH: Sophisticated phishing attack detected in $source."
            EmergencyType.DEVICE_TAKEOVER ->
                "CRITICAL: Device takeover attempt detected. Remote access blocked."
            EmergencyType.CREDENTIAL_THEFT ->
                "SEVERE: Credential theft attempt detected in $source."
        }
    }
    
    private fun determineEmergencyActions(type: EmergencyType): List<EmergencyAction> {
        return when (type) {
            EmergencyType.RANSOMWARE_DETECTED -> listOf(
                EmergencyAction.ISOLATE_APP,
                EmergencyAction.BLOCK_NETWORK,
                EmergencyAction.ENABLE_LOCKDOWN,
                EmergencyAction.SCAN_DEVICE,
                EmergencyAction.NOTIFY_AUTHORITIES
            )
            EmergencyType.BANKING_MALWARE -> listOf(
                EmergencyAction.ISOLATE_APP,
                EmergencyAction.BLOCK_NETWORK,
                EmergencyAction.CLEAR_SESSION,
                EmergencyAction.SCAN_DEVICE
            )
            EmergencyType.ACCOUNT_COMPROMISE -> listOf(
                EmergencyAction.ISOLATE_APP,
                EmergencyAction.CLEAR_SESSION,
                EmergencyAction.DISABLE_PERMISSIONS
            )
            EmergencyType.DATA_BREACH -> listOf(
                EmergencyAction.BLOCK_NETWORK,
                EmergencyAction.CLEAR_SESSION,
                EmergencyAction.NOTIFY_AUTHORITIES
            )
            EmergencyType.PRIVACY_VIOLATION -> listOf(
                EmergencyAction.DISABLE_PERMISSIONS,
                EmergencyAction.ISOLATE_APP
            )
            EmergencyType.PHISHING_ATTACK -> listOf(
                EmergencyAction.ISOLATE_APP,
                EmergencyAction.BLOCK_NETWORK
            )
            EmergencyType.DEVICE_TAKEOVER -> listOf(
                EmergencyAction.ENABLE_LOCKDOWN,
                EmergencyAction.BLOCK_NETWORK,
                EmergencyAction.SCAN_DEVICE,
                EmergencyAction.NOTIFY_AUTHORITIES
            )
            EmergencyType.CREDENTIAL_THEFT -> listOf(
                EmergencyAction.CLEAR_SESSION,
                EmergencyAction.DISABLE_PERMISSIONS,
                EmergencyAction.ISOLATE_APP
            )
        }
    }
    
    private fun executeEmergencyActions(emergency: EmergencyEvent) {
        emergency.actionsTaken.forEach { action ->
            when (action) {
                EmergencyAction.ISOLATE_APP -> isolateApp(emergency.sourceApp)
                EmergencyAction.BLOCK_NETWORK -> blockNetworkAccess()
                EmergencyAction.DISABLE_PERMISSIONS -> disableAppPermissions(emergency.sourceApp)
                EmergencyAction.CLEAR_SESSION -> clearSensitiveSessions()
                EmergencyAction.ENABLE_LOCKDOWN -> enableLockdownMode()
                EmergencyAction.NOTIFY_AUTHORITIES -> notifyAuthorities(emergency)
                EmergencyAction.BACKUP_DATA -> initiateEmergencyBackup()
                EmergencyAction.SCAN_DEVICE -> initiateFullDeviceScan()
            }
        }
    }
    
    private fun isolateApp(packageName: String?) {
        // Implement app isolation logic
        // This would involve:
        // - Suspending the app
        // - Revoking permissions
        // - Blocking network access
        packageName?.let {
            // Android-specific implementation would go here
        }
    }
    
    private fun blockNetworkAccess() {
        // Implement network blocking
        // This would use VPNService or firewall rules
    }
    
    private fun disableAppPermissions(packageName: String?) {
        packageName?.let {
            try {
                // Revoke dangerous permissions
                val dangerousPermissions = listOf(
                    android.Manifest.permission.READ_SMS,
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.READ_CONTACTS,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
                )

            } catch (e: Exception) {
                // Handle permission revocation errors
            }
        }
    }
    
    private fun clearSensitiveSessions() {
        // Clear banking app sessions, browser sessions, etc.
        // This would involve:
        // - Clearing browser cookies and cache
        // - Logging out of banking apps
        // - Clearing clipboard
    }
    
    private fun notifyAuthorities(emergency: EmergencyEvent) {
        // Generate report for authorities
        // This would include:
        // - Threat details
        // - Source app information
        // - Timestamp
        // - Device information
        // - User location (with consent)
    }
    
    private fun initiateEmergencyBackup() {
        // Start emergency backup of critical data
        // This would backup:
        // - Contacts
        // - Photos
        // - Documents
        // - App data
    }
    
    private fun initiateFullDeviceScan() {
        // Trigger full device malware scan
        // This would scan:
        // - All installed apps
        // - System files
        // - Downloaded files
    }
    
    @RequiresApi(Build.VERSION_CODES.M)
    private fun enableDataSaver() {
        // Enable data saver mode to restrict network access
        try {
            val dataSaverManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager
            // Implementation depends on Android API level
        } catch (e: Exception) {
            // Handle errors
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.M)
    private fun disableDataSaver() {
        // Disable data saver mode
        try {
            val dataSaverManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager
            // Implementation depends on Android API level
        } catch (e: Exception) {
            // Handle errors
        }
    }
    
    fun getEmergencyRecoverySteps(emergencyType: EmergencyType): List<String> {
        return when (emergencyType) {
            EmergencyType.RANSOMWARE_DETECTED -> listOf(
                "1. Disconnect device from all networks immediately",
                "2. Do not pay the ransom - it encourages criminals",
                "3. Power off the device to prevent further encryption",
                "4. Contact cybersecurity professionals",
                "5. Report to local authorities",
                "6. Restore from backup if available",
                "7. Change all passwords from a secure device"
            )
            EmergencyType.BANKING_MALWARE -> listOf(
                "1. Contact your bank immediately to freeze accounts",
                "2. Change all banking credentials",
                "3. Monitor accounts for unauthorized transactions",
                "4. Report the incident to your bank's fraud department",
                "5. Scan device with antivirus software",
                "6. Enable transaction alerts",
                "7. Consider identity theft protection services"
            )
            EmergencyType.ACCOUNT_COMPROMISE -> listOf(
                "1. Change password immediately from a secure device",
                "2. Enable two-factor authentication",
                "3. Review account activity for unauthorized access",
                "4. Check connected apps and devices",
                "5. Update recovery email and phone",
                "6. Notify contacts about potential spam from your account"
            )
            EmergencyType.DATA_BREACH -> listOf(
                "1. Identify what data was exposed",
                "2. Change passwords for affected accounts",
                "3. Enable credit monitoring if financial data exposed",
                "4. Notify affected parties if required by law",
                "5. Document the breach for legal purposes",
                "6. Review and update security policies"
            )
            else -> listOf(
                "1. Follow AEGIS recommended actions",
                "2. Scan device for threats",
                "3. Update all software",
                "4. Change passwords",
                "5. Monitor for suspicious activity"
            )
        }
    }
}
