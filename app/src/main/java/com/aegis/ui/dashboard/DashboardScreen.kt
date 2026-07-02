package com.aegis.ui.dashboard

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.ThreatRepository
import com.aegis.ui.components.*
import com.aegis.ui.navigation.Screen
import com.aegis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    guardianCore: GuardianCore,
    safetyRepository: SafetyRepository,
    threatRepository: ThreatRepository,
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(
                    guardianCore, safetyRepository, threatRepository
                ) as T
            }
        }
    )
) {
    val safetyScore by viewModel.safetyScore.collectAsState()
    val recentThreats by viewModel.recentThreats.collectAsState()
    val activeThreatCount by viewModel.activeThreatCount.collectAsState()
    val totalScans by viewModel.totalScans.collectAsState()
    val agentStatuses by viewModel.agentStatuses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("AEGIS", fontWeight = FontWeight.Bold)
                },
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
            item {
                GradientTopBar()
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SafetyScoreCircle(score = safetyScore, size = 140)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your device is ${if (safetyScore >= 0.8f) "Protected" else if (safetyScore >= 0.5f) "At Risk" else "Vulnerable"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(safetyScore * 100).toInt()}% Safety Score",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Threats",
                        value = activeThreatCount.toString(),
                        color = if (activeThreatCount > 0) DangerRed else SafeGreen
                    )
                    StatItem(
                        label = "Scans",
                        value = totalScans.toString()
                    )
                    StatItem(
                        label = "Agents",
                        value = "${guardianCore.availableAgentCount}/${guardianCore.agentCount}"
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Quick Actions")
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureCard(
                        icon = { Icon(Icons.Filled.SmartToy, null, tint = CyberBlue, modifier = Modifier.size(24.dp)) },
                        title = "AI Assistant",
                        description = "Ask about cybersecurity",
                        onClick = { navController.navigate(Screen.Assistant.route) },
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = { Icon(Icons.Filled.Warning, null, tint = WarningOrange, modifier = Modifier.size(24.dp)) },
                        title = "Threat Log",
                        description = "View security events",
                        onClick = { navController.navigate(Screen.ThreatLog.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeatureCard(
                        icon = { Icon(Icons.Filled.School, null, tint = CyberPurple, modifier = Modifier.size(24.dp)) },
                        title = "Academy",
                        description = "Learn cybersecurity",
                        onClick = { navController.navigate(Screen.Academy.route) },
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = { Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(24.dp)) },
                        title = "Settings",
                        description = "Configure protection",
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Agent Status")
            }

            items(agentStatuses) { agent ->
                AgentStatusCard(
                    name = agent.name,
                    isActive = agent.isAvailable,
                    description = agent.description,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            if (recentThreats.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader("Recent Threats")
                }
                item {
                    ThreatTimeline(threats = recentThreats)
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
