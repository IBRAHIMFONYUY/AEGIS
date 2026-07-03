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
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val safetyScoreData by viewModel.safetyScore.collectAsState()
    val overallScore = safetyScoreData?.score ?: 1.0f
    
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
                    SafetyScoreCircle(score = overallScore, size = 140)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your device is ${if (overallScore >= 0.8f) "Protected" else if (overallScore >= 0.5f) "At Risk" else "Vulnerable"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(overallScore * 100).toInt()}% Guardian Score",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            item {
                if (safetyScoreData != null) {
                    GuardianScoreGrid(safetyScoreData!!)
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

@Composable
fun GuardianScoreGrid(score: com.aegis.data.db.entity.SafetyScore) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoreMiniCard(label = "Privacy", score = score.privacyScore, modifier = Modifier.weight(1f))
            ScoreMiniCard(label = "Scams", score = score.scamScore, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoreMiniCard(label = "Device", score = score.deviceScore, modifier = Modifier.weight(1f))
            ScoreMiniCard(label = "Wellbeing", score = score.wellbeingScore, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ScoreMiniCard(label: String, score: Float, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            LinearProgressIndicator(
                progress = score,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                color = if (score >= 0.8f) SafeGreen else if (score >= 0.5f) WarningOrange else DangerRed
            )
            Text(text = "${(score * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}
