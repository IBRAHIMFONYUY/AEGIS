package com.aegis.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FamilyRole(val displayName: String, val permissions: List<FamilyPermission>) {
    ADMIN("Administrator", listOf(
        FamilyPermission.VIEW_ALL,
        FamilyPermission.MANAGE_MEMBERS,
        FamilyPermission.VIEW_ALERTS,
        FamilyPermission.MANAGE_SETTINGS,
        FamilyPermission.RECEIVE_NOTIFICATIONS
    )),
    GUARDIAN("Guardian", listOf(
        FamilyPermission.VIEW_ALL,
        FamilyPermission.VIEW_ALERTS,
        FamilyPermission.RECEIVE_NOTIFICATIONS
    )),
    MEMBER("Member", listOf(
        FamilyPermission.VIEW_OWN,
        FamilyPermission.RECEIVE_NOTIFICATIONS
    )),
    CHILD("Child", listOf(
        FamilyPermission.VIEW_OWN
    ))
}

enum class FamilyPermission(val displayName: String) {
    VIEW_ALL("View all family data"),
    VIEW_OWN("View own data only"),
    MANAGE_MEMBERS("Add/remove family members"),
    VIEW_ALERTS("View security alerts"),
    MANAGE_SETTINGS("Manage family settings"),
    RECEIVE_NOTIFICATIONS("Receive threat notifications")
}

data class FamilyMember(
    val id: String,
    val name: String,
    val role: FamilyRole,
    val deviceId: String?,
    val guardianScore: GuardianScore,
    val lastActive: Long,
    val isOnline: Boolean,
    val protectionEnabled: Boolean
)

data class FamilyAlert(
    val id: String,
    val memberId: String,
    val memberName: String,
    val threatType: String,
    val severity: ThreatLevel,
    val description: String,
    val timestamp: Long,
    val resolved: Boolean = false
)

data class FamilySettings(
    val alertThreshold: ThreatLevel = ThreatLevel.SUSPICIOUS,
    val locationSharing: Boolean = false,
    val appBlockingEnabled: Boolean = false,
    val screenTimeMonitoring: Boolean = false,
    val emergencyContacts: List<String> = emptyList()
)

class FamilyProtectionManager {
    
    private val _familyMembers = MutableStateFlow(listOf<FamilyMember>())
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()
    
    private val _familyAlerts = MutableStateFlow(listOf<FamilyAlert>())
    val familyAlerts: StateFlow<List<FamilyAlert>> = _familyAlerts.asStateFlow()
    
    private val _familySettings = MutableStateFlow(FamilySettings())
    val familySettings: StateFlow<FamilySettings> = _familySettings.asStateFlow()
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    
    fun createFamily(adminName: String, deviceId: String): String {
        val adminId = "FAMILY-${System.currentTimeMillis()}"
        val admin = FamilyMember(
            id = adminId,
            name = adminName,
            role = FamilyRole.ADMIN,
            deviceId = deviceId,
            guardianScore = GuardianScore(1f, 1f, 1f, 1f, 1f),
            lastActive = System.currentTimeMillis(),
            isOnline = true,
            protectionEnabled = true
        )
        
        _familyMembers.value = listOf(admin)
        _currentUserId.value = adminId
        
        return adminId
    }
    
    fun addMember(
        name: String,
        role: FamilyRole,
        deviceId: String?,
        inviterId: String
    ): Boolean {
        val inviter = _familyMembers.value.find { it.id == inviterId }
        if (inviter?.role != FamilyRole.ADMIN && inviter?.role != FamilyRole.GUARDIAN) {
            return false
        }
        
        val memberId = "MEMBER-${System.currentTimeMillis()}"
        val newMember = FamilyMember(
            id = memberId,
            name = name,
            role = role,
            deviceId = deviceId,
            guardianScore = GuardianScore(1f, 1f, 1f, 1f, 1f),
            lastActive = System.currentTimeMillis(),
            isOnline = deviceId != null,
            protectionEnabled = true
        )
        
        _familyMembers.value = _familyMembers.value + newMember
        return true
    }
    
    fun removeMember(memberId: String, requesterId: String): Boolean {
        val requester = _familyMembers.value.find { it.id == requesterId }
        if (requester?.role != FamilyRole.ADMIN) {
            return false
        }
        
        if (memberId == requesterId) {
            return false // Cannot remove admin
        }
        
        _familyMembers.value = _familyMembers.value.filter { it.id != memberId }
        return true
    }
    
    fun updateMemberRole(memberId: String, newRole: FamilyRole, requesterId: String): Boolean {
        val requester = _familyMembers.value.find { it.id == requesterId }
        if (requester?.role != FamilyRole.ADMIN) {
            return false
        }
        
        val members = _familyMembers.value.toMutableList()
        val index = members.indexOfFirst { it.id == memberId }
        if (index >= 0) {
            members[index] = members[index].copy(role = newRole)
            _familyMembers.value = members
            return true
        }
        
        return false
    }
    
    fun updateMemberScore(memberId: String, score: GuardianScore) {
        val members = _familyMembers.value.toMutableList()
        val index = members.indexOfFirst { it.id == memberId }
        if (index >= 0) {
            members[index] = members[index].copy(
                guardianScore = score,
                lastActive = System.currentTimeMillis()
            )
            _familyMembers.value = members
        }
    }
    
    fun updateMemberOnlineStatus(memberId: String, isOnline: Boolean) {
        val members = _familyMembers.value.toMutableList()
        val index = members.indexOfFirst { it.id == memberId }
        if (index >= 0) {
            members[index] = members[index].copy(
                isOnline = isOnline,
                lastActive = System.currentTimeMillis()
            )
            _familyMembers.value = members
        }
    }
    
    fun toggleProtection(memberId: String, enabled: Boolean, requesterId: String): Boolean {
        val requester = _familyMembers.value.find { it.id == requesterId }
        val member = _familyMembers.value.find { it.id == memberId }
        
        if (requester == null || member == null) {
            return false
        }
        
        // Admins can toggle anyone's protection
        // Guardians can toggle children's protection
        // Members can only toggle their own
        val canToggle = when (requester.role) {
            FamilyRole.ADMIN -> true
            FamilyRole.GUARDIAN -> member.role == FamilyRole.CHILD
            FamilyRole.MEMBER -> requester.id == memberId
            FamilyRole.CHILD -> false
        }
        
        if (!canToggle) {
            return false
        }
        
        val members = _familyMembers.value.toMutableList()
        val index = members.indexOfFirst { it.id == memberId }
        if (index >= 0) {
            members[index] = members[index].copy(protectionEnabled = enabled)
            _familyMembers.value = members
            return true
        }
        
        return false
    }
    
    fun createAlert(
        memberId: String,
        threatType: String,
        severity: ThreatLevel,
        description: String
    ) {
        val member = _familyMembers.value.find { it.id == memberId }
        if (member == null) return
        
        // Only create alert if severity meets threshold
        if (severity.value < _familySettings.value.alertThreshold.value) {
            return
        }
        
        val alert = FamilyAlert(
            id = "ALERT-${System.currentTimeMillis()}",
            memberId = memberId,
            memberName = member.name,
            threatType = threatType,
            severity = severity,
            description = description,
            timestamp = System.currentTimeMillis()
        )
        
        _familyAlerts.value = listOf(alert) + _familyAlerts.value
    }
    
    fun resolveAlert(alertId: String, requesterId: String): Boolean {
        val requester = _familyMembers.value.find { it.id == requesterId }
        if (requester == null) return false
        
        val alerts = _familyAlerts.value.toMutableList()
        val index = alerts.indexOfFirst { it.id == alertId }
        if (index >= 0) {
            alerts[index] = alerts[index].copy(resolved = true)
            _familyAlerts.value = alerts
            return true
        }
        
        return false
    }
    
    fun updateSettings(newSettings: FamilySettings, requesterId: String): Boolean {
        val requester = _familyMembers.value.find { it.id == requesterId }
        if (requester?.role != FamilyRole.ADMIN) {
            return false
        }
        
        _familySettings.value = newSettings
        return true
    }
    
    fun getFamilyHealthScore(): Float {
        val members = _familyMembers.value
        if (members.isEmpty()) return 1f
        
        val avgScore = members.map { it.guardianScore.overall }.average().toFloat()
        val protectionRate = members.count { it.protectionEnabled }.toFloat() / members.size
        
        return (avgScore * 0.7f + protectionRate * 0.3f).coerceIn(0f, 1f)
    }
    
    fun getActiveAlertsCount(): Int {
        return _familyAlerts.value.count { !it.resolved }
    }
    
    fun getMemberById(memberId: String): FamilyMember? {
        return _familyMembers.value.find { it.id == memberId }
    }
    
    fun getCurrentUser(): FamilyMember? {
        val userId = _currentUserId.value ?: return null
        return _familyMembers.value.find { it.id == userId }
    }
    
    fun hasPermission(userId: String, permission: FamilyPermission): Boolean {
        val member = _familyMembers.value.find { it.id == userId } ?: return false
        return permission in member.role.permissions
    }
    
    fun getFamilyStatistics(): FamilyStatistics {
        val members = _familyMembers.value
        val alerts = _familyAlerts.value
        
        return FamilyStatistics(
            totalMembers = members.size,
            onlineMembers = members.count { it.isOnline },
            protectedMembers = members.count { it.protectionEnabled },
            averageGuardianScore = members.map { it.guardianScore.overall }.average().toFloat(),
            activeAlerts = alerts.count { !it.resolved },
            resolvedAlerts = alerts.count { it.resolved },
            criticalAlerts = alerts.count { !it.resolved && it.severity == ThreatLevel.CRITICAL }
        )
    }
}

data class FamilyStatistics(
    val totalMembers: Int,
    val onlineMembers: Int,
    val protectedMembers: Int,
    val averageGuardianScore: Float,
    val activeAlerts: Int,
    val resolvedAlerts: Int,
    val criticalAlerts: Int
)
