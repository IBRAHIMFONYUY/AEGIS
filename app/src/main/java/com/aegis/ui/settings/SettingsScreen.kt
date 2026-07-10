package com.aegis.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.SettingsRepository
import com.aegis.ui.components.*
import com.aegis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    guardianCore: GuardianCore,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val services by viewModel.services.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isRealTimeScanning by viewModel.isRealTimeScanning.collectAsState()
    val isBackgroundScanning by viewModel.isBackgroundScanning.collectAsState()
    val agentVersions by viewModel.agentVersions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { GradientTopBar() }

            item { SectionHeader("Protection Services") }

            items(services) { service ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = AegisCardShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = service.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = service.isEnabled,
                            onCheckedChange = { enabled ->
                                when (service.name) {
                                    "Accessibility Service" -> viewModel.openAccessibilitySettings()
                                    "Notification Listener" -> viewModel.openNotificationListenerSettings()
                                    "VPN Service" -> viewModel.openVpnSettings()
                                    "Foreground Service" -> viewModel.startForegroundService()
                                    "Clipboard Monitor" -> {
                                        viewModel.toggleClipboardMonitor(enabled)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AegisPrimaryLight,
                                checkedTrackColor = AegisPrimaryLight.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Scanning Options")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = AegisCardShape
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Real-time Scanning", style = MaterialTheme.typography.titleSmall)
                                Text("Analyze text as you type", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Switch(
                                checked = isRealTimeScanning,
                                onCheckedChange = { viewModel.toggleRealTimeScanning(it) }
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Background Scanning", style = MaterialTheme.typography.titleSmall)
                                Text("Periodic threat scans in background", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Switch(
                                checked = isBackgroundScanning,
                                onCheckedChange = { viewModel.toggleBackgroundScanning(it) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("AI Agent Versions")
            }

            items(agentVersions) { agent ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = AegisCardShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = agent.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = agent.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = "v${agent.version}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("About")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = AegisCardShape
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AEGIS v1.0.0", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Advanced AI-Powered Mobile Security",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Built for Cameroon and the world. All AI processing is done on-device. No data leaves your phone without your permission.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
