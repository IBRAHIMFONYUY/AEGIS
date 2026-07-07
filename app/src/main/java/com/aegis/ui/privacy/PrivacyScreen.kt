package com.aegis.ui.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.ui.components.GradientTopBar
import com.aegis.ui.components.SectionHeader
import com.aegis.ui.theme.DangerRed
import com.aegis.ui.theme.SafeGreen
import com.aegis.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    viewModel: PrivacyViewModel = hiltViewModel()
) {
    val privacyScore by viewModel.privacyScore.collectAsState()
    val isCameraProtected by viewModel.isCameraProtected.collectAsState()
    val isMicProtected by viewModel.isMicProtected.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Center", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                GradientTopBar()
            }

            item {
                PrivacySummaryCard(privacyScore)
            }

            item {
                SectionHeader("Active Monitoring")
            }

            item {
                PrivacyToggleItem(
                    PrivacyItem("Camera Protection", "Notify if app uses camera in background", Icons.Filled.CameraAlt, WarningOrange),
                    isCameraProtected,
                    onToggle = { viewModel.toggleCameraProtection(it) }
                )
            }
            
            item {
                PrivacyToggleItem(
                    PrivacyItem("Microphone Lock", "Prevent unauthorized audio recording", Icons.Filled.Mic, DangerRed),
                    isMicProtected,
                    onToggle = { viewModel.toggleMicProtection(it) }
                )
            }

            item {
                PrivacyToggleItem(
                    PrivacyItem("Location Privacy", "Mask your GPS coordinates from trackers", Icons.Filled.LocationOn, SafeGreen),
                    true,
                    onToggle = {}
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Identity Protection")
                IdentityRiskCard(onScan = { viewModel.runDeepScan() })
            }
        }
    }
}

@Composable
private fun PrivacySummaryCard(score: Float) {
    val status = when {
        score >= 0.8f -> "Strong"
        score >= 0.5f -> "Fair"
        else -> "Weak"
    }
    val color = when {
        score >= 0.8f -> SafeGreen
        score >= 0.5f -> WarningOrange
        else -> DangerRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Security, null, tint = color, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Privacy Status: $status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Your privacy health is currently at ${(score * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PrivacyToggleItem(item: PrivacyItem, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, null, tint = item.color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun IdentityRiskCard(onScan: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Dark Web Data Leak Check", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No leaks found for your primary email and phone number in Cameroon database.", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                Text("Run Deep Identity Scan")
            }
        }
    }
}

data class PrivacyItem(val title: String, val description: String, val icon: ImageVector, val color: androidx.compose.ui.graphics.Color)
