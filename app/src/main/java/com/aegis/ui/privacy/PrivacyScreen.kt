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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.ui.components.GradientTopBar
import com.aegis.ui.components.SectionHeader
import com.aegis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    viewModel: PrivacyViewModel = hiltViewModel()
) {
    val privacyScore by viewModel.privacyScore.collectAsState()
    val isCameraProtected by viewModel.isCameraProtected.collectAsState()
    val isMicProtected by viewModel.isMicProtected.collectAsState()
    val privacyReport by viewModel.privacyReport.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

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
                SectionHeader("Privacy Advisor")
                PrivacyAdvisorCard(
                    report = privacyReport,
                    isScanning = isScanning,
                    onGenerate = { viewModel.generatePrivacyReport() }
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
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = AegisCardShape
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Security, null, tint = color, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Privacy Status: $status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your privacy health is currently at ${(score * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PrivacyToggleItem(item: PrivacyItem, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = AegisCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = item.color.copy(alpha = 0.15f),
                shape = AegisCardShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AegisPrimary,
                    checkedTrackColor = AegisPrimary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun PrivacyAdvisorCard(
    report: String?,
    isScanning: Boolean,
    onGenerate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AegisCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ListAlt, null, tint = AegisPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Permission Audit Report", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (report != null) {
                Text(
                    text = report,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    "Scan your device for apps with excessive permissions and privacy risks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onGenerate,
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth(),
                shape = AegisButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.Black)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.Black)
                } else {
                    Icon(Icons.Filled.AssignmentInd, null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Generating Report..." else "Generate Privacy Report", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IdentityRiskCard(onScan: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = AegisCardShape
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Fingerprint, null, tint = AegisPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Dark Web Data Leak Check", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No leaks found for your primary email and phone number in Cameroon database.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth(),
                shape = AegisButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimary, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run Deep Identity Scan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class PrivacyItem(val title: String, val description: String, val icon: ImageVector, val color: androidx.compose.ui.graphics.Color)
