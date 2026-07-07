package com.aegis.ui.family

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.core.*
import com.aegis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyProtectionScreen(
    familyManager: FamilyProtectionManager
) {
    val familyMembers by familyManager.familyMembers.collectAsState()
    val familyAlerts by familyManager.familyAlerts.collectAsState()
    val familySettings by familyManager.familySettings.collectAsState()
    val currentUser by remember { derivedStateOf { familyManager.getCurrentUser() } }
    
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val familyStats by remember { derivedStateOf { familyManager.getFamilyStatistics() } }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Family Protection",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${familyMembers.size} members protected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMemberDialog = true }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add Member")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Family Health Overview
            item {
                FamilyHealthCard(
                    healthScore = familyManager.getFamilyHealthScore(),
                    stats = familyStats
                )
            }
            
            // Active Alerts
            if (familyStats.activeAlerts > 0) {
                item {
                    SectionHeader("Active Alerts (${familyStats.activeAlerts})")
                }
                
                items(
                    items = familyAlerts.filter { !it.resolved }.take(5),
                    key = { it.id }
                ) { alert ->
                    FamilyAlertCard(
                        alert = alert,
                        onResolve = { 
                            currentUser?.id?.let { familyManager.resolveAlert(alert.id, it) }
                        },
                        canResolve = currentUser?.id?.let {
                            familyManager.hasPermission(it, FamilyPermission.VIEW_ALERTS)
                        } ?: false
                    )
                }
            }
            
            // Family Members
            item {
                SectionHeader("Family Members")
            }
            
            items(
                items = familyMembers,
                key = { it.id }
            ) { member ->
                FamilyMemberCard(
                    member = member,
                    isCurrentUser = member.id == currentUser?.id,
                    currentUserRole = currentUser?.role,
                    onToggleProtection = { enabled ->
                        currentUser?.id?.let { 
                            familyManager.toggleProtection(member.id, enabled, it)
                        }
                    },
                    canToggleProtection = currentUser?.let { user ->
                        when (user.role) {
                            FamilyRole.ADMIN -> true
                            FamilyRole.GUARDIAN -> member.role == FamilyRole.CHILD
                            FamilyRole.MEMBER -> user.id == member.id
                            FamilyRole.CHILD -> false
                        }
                    } ?: false
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAdd = { name, role ->
                currentUser?.id?.let { 
                    familyManager.addMember(name, role, null, it)
                }
                showAddMemberDialog = false
            },
            currentUserRole = currentUser?.role
        )
    }
    
    if (showSettingsDialog) {
        FamilySettingsDialog(
            currentSettings = familySettings,
            onDismiss = { showSettingsDialog = false },
            onSave = { newSettings ->
                currentUser?.id?.let { 
                    familyManager.updateSettings(newSettings, it)
                }
                showSettingsDialog = false
            },
            canEdit = currentUser?.id?.let {
                familyManager.hasPermission(it, FamilyPermission.MANAGE_SETTINGS)
            } ?: false
        )
    }
}

@Composable
private fun FamilyHealthCard(
    healthScore: Float,
    stats: FamilyStatistics
) {
    val animatedScore by animateFloatAsState(
        targetValue = healthScore * 100,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "healthScore"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                healthScore >= 0.8f -> SafeGreen.copy(alpha = 0.1f)
                healthScore >= 0.6f -> CyberBlue.copy(alpha = 0.1f)
                else -> WarningOrange.copy(alpha = 0.1f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Family Health Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Overall protection status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                healthScore >= 0.8f -> SafeGreen.copy(alpha = 0.2f)
                                healthScore >= 0.6f -> CyberBlue.copy(alpha = 0.2f)
                                else -> WarningOrange.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${animatedScore.toInt()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                healthScore >= 0.8f -> SafeGreen
                                healthScore >= 0.6f -> CyberBlue
                                else -> WarningOrange
                            }
                        )
                        Text(
                            "/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Members", stats.totalMembers.toString())
                StatItem("Online", stats.onlineMembers.toString())
                StatItem("Protected", stats.protectedMembers.toString())
                StatItem("Alerts", stats.activeAlerts.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun FamilyAlertCard(
    alert: FamilyAlert,
    onResolve: () -> Unit,
    canResolve: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                ThreatLevel.CRITICAL -> DangerRed.copy(alpha = 0.1f)
                ThreatLevel.MALICIOUS -> WarningOrange.copy(alpha = 0.1f)
                else -> CyberBlue.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = when (alert.severity) {
                            ThreatLevel.CRITICAL -> DangerRed
                            ThreatLevel.MALICIOUS -> WarningOrange
                            else -> CyberBlue
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            alert.memberName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            alert.threatType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Text(
                    "${alert.severity.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (alert.severity) {
                        ThreatLevel.CRITICAL -> DangerRed
                        ThreatLevel.MALICIOUS -> WarningOrange
                        else -> CyberBlue
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                alert.description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (canResolve) {
                Button(
                    onClick = onResolve,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Resolved")
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberCard(
    member: FamilyMember,
    isCurrentUser: Boolean,
    currentUserRole: FamilyRole?,
    onToggleProtection: (Boolean) -> Unit,
    canToggleProtection: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (member.isOnline) SafeGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = if (member.isOnline) SafeGreen else Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            member.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(You)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        member.role.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Score: ${(member.guardianScore.overall * 100).toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                member.guardianScore.overall >= 0.8f -> SafeGreen
                                member.guardianScore.overall >= 0.6f -> CyberBlue
                                else -> WarningOrange
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (member.isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (member.isOnline) SafeGreen else Color.Gray
                        )
                    }
                }
            }
            
            Switch(
                checked = member.protectionEnabled,
                onCheckedChange = onToggleProtection,
                enabled = canToggleProtection
            )
        }
    }
}

@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (String, FamilyRole) -> Unit,
    currentUserRole: FamilyRole?
) {
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(FamilyRole.MEMBER) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Family Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Role:")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FamilyRole.entries.forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(role.displayName)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, selectedRole) },
                enabled = name.isNotBlank()
            ) {
                Text("Add Member")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FamilySettingsDialog(
    currentSettings: FamilySettings,
    onDismiss: () -> Unit,
    onSave: (FamilySettings) -> Unit,
    canEdit: Boolean
) {
    var alertThreshold by remember { mutableStateOf(currentSettings.alertThreshold) }
    var locationSharing by remember { mutableStateOf(currentSettings.locationSharing) }
    var appBlockingEnabled by remember { mutableStateOf(currentSettings.appBlockingEnabled) }
    var screenTimeMonitoring by remember { mutableStateOf(currentSettings.screenTimeMonitoring) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Family Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Alert Threshold:")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThreatLevel.entries.forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = alertThreshold == level,
                                onClick = { if (canEdit) alertThreshold = level },
                                enabled = canEdit
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(level.name)
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = locationSharing,
                        onCheckedChange = { if (canEdit) locationSharing = it },
                        enabled = canEdit
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Location Sharing")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = appBlockingEnabled,
                        onCheckedChange = { if (canEdit) appBlockingEnabled = it },
                        enabled = canEdit
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("App Blocking")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = screenTimeMonitoring,
                        onCheckedChange = { if (canEdit) screenTimeMonitoring = it },
                        enabled = canEdit
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Screen Time Monitoring")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        FamilySettings(
                            alertThreshold = alertThreshold,
                            locationSharing = locationSharing,
                            appBlockingEnabled = appBlockingEnabled,
                            screenTimeMonitoring = screenTimeMonitoring,
                            emergencyContacts = currentSettings.emergencyContacts
                        )
                    )
                },
                enabled = canEdit
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
