package com.aegis.security

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionAuditor(private val context: Context) {

    data class PermissionStatus(
        val permission: String,
        val isGranted: Boolean,
        val isCritical: Boolean,
        val riskLevel: RiskLevel
    )

    enum class RiskLevel(val value: Int) {
        LOW(0), MEDIUM(1), HIGH(2), CRITICAL(3)
    }

    private val criticalPermissions = mapOf(
        android.Manifest.permission.READ_SMS to RiskLevel.CRITICAL,
        android.Manifest.permission.RECEIVE_SMS to RiskLevel.CRITICAL,
        android.Manifest.permission.CAMERA to RiskLevel.HIGH,
        android.Manifest.permission.RECORD_AUDIO to RiskLevel.HIGH,
        android.Manifest.permission.ACCESS_FINE_LOCATION to RiskLevel.HIGH,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION to RiskLevel.CRITICAL,
        android.Manifest.permission.READ_CONTACTS to RiskLevel.HIGH,
        android.Manifest.permission.READ_CALL_LOG to RiskLevel.HIGH,
        android.Manifest.permission.READ_EXTERNAL_STORAGE to RiskLevel.MEDIUM,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE to RiskLevel.HIGH,
        android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE to RiskLevel.CRITICAL,
        android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE to RiskLevel.HIGH,
        android.Manifest.permission.BIND_VPN_SERVICE to RiskLevel.MEDIUM,
        android.Manifest.permission.POST_NOTIFICATIONS to RiskLevel.LOW
    )

    fun auditGrantedPermissions(): List<PermissionStatus> {
        return criticalPermissions.map { (permission, risk) ->
            val granted = ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
            PermissionStatus(
                permission = permission,
                isGranted = granted,
                isCritical = risk == RiskLevel.CRITICAL,
                riskLevel = risk
            )
        }
    }

    fun getHighRiskPermissions(): List<PermissionStatus> {
        return auditGrantedPermissions().filter { it.isGranted && it.riskLevel.value >= RiskLevel.HIGH.value }
    }

    fun generatePermissionReport(): String {
        val statuses = auditGrantedPermissions()
        val granted = statuses.filter { it.isGranted }
        val denied = statuses.filter { !it.isGranted }
        val critical = granted.filter { it.riskLevel == RiskLevel.CRITICAL }

        return buildString {
            appendLine("=== AEGIS Permission Audit ===")
            appendLine("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            appendLine()
            appendLine("Granted Permissions: ${granted.size}/${statuses.size}")
            appendLine("Critical Permissions Granted: ${critical.size}")
            appendLine()
            if (critical.isNotEmpty()) {
                appendLine("⚠ CRITICAL PERMISSIONS ACTIVE:")
                critical.forEach {
                    appendLine("  - ${it.permission.substringAfterLast('.')}")
                }
                appendLine()
            }
            granted.forEach {
                appendLine("[${if (it.riskLevel == RiskLevel.CRITICAL) "⚠" else "✓"}] ${it.permission.substringAfterLast('.')} (${it.riskLevel})")
            }
        }
    }
}
