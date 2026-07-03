package com.aegis.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.ThreatRepository
import com.aegis.ui.components.*
import com.aegis.ui.navigation.Screen
import com.aegis.ui.theme.*
import java.util.*

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
    val userName by viewModel.userName.collectAsState()
    val protectionStats by viewModel.protectionStats.collectAsState()
    val recentThreats by viewModel.recentThreats.collectAsState()

    val greeting = remember {
        val calendar = Calendar.getInstance()
        when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$greeting, $userName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AEGIS is watching your back",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Profile */ }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                GradientTopBar()
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Am I safe right now?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(contentAlignment = Alignment.Center) {
                        SafetyScoreCircle(score = overallScore, size = 200, strokeWidth = 16f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (overallScore >= 0.8f) Icons.Filled.Shield else Icons.Filled.ReportProblem,
                                contentDescription = null,
                                tint = if (overallScore >= 0.8f) SafeGreen else WarningOrange,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (overallScore >= 0.8f) "PROTECTED" else "AT RISK",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (overallScore >= 0.8f) SafeGreen else WarningOrange
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItemSmall(label = "Current Risk", value = if (overallScore >= 0.8f) "LOW" else "MEDIUM", color = if (overallScore >= 0.8f) SafeGreen else WarningOrange)
                        VerticalDivider(modifier = Modifier.height(40.dp))
                        StatItemSmall(label = "Health", value = "${(overallScore * 100).toInt()}%", color = CyberBlue)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { viewModel.refreshData() },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        shape = MaterialTheme.shapes.extraLarge,
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.Filled.Security, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Guardian Scan", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                SectionHeader("Today's Protection")
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProtectionLogItem(text = "${protectionStats.scamsBlocked} Scam messages blocked", isSuccess = true)
                        ProtectionLogItem(text = "${protectionStats.linksBlocked} Dangerous links blocked", isSuccess = true)
                        ProtectionLogItem(text = "${protectionStats.fakeNewsDetected} Fake news detected", isSuccess = true)
                        ProtectionLogItem(text = "Microphone secured", isSuccess = protectionStats.micSecured)
                        ProtectionLogItem(text = "Camera secured", isSuccess = protectionStats.cameraSecured)
                    }
                }
            }

            if (recentThreats.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader("Recent Alerts")
                }
                items(recentThreats) { threat ->
                    AlertItem(threat = threat)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Quick Actions")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionSquare(icon = Icons.Filled.Shield, label = "Shield", color = SafeGreen, modifier = Modifier.weight(1f))
                    ActionSquare(icon = Icons.Filled.Https, label = "Vault", color = WarningOrange, modifier = Modifier.weight(1f))
                    ActionSquare(icon = Icons.Filled.SmartToy, label = "AI Guardian", color = CyberBlue, modifier = Modifier.weight(1f), onClick = { navController.navigate(Screen.Assistant.route) })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionSquare(icon = Icons.Filled.Lock, label = "Privacy", color = CyberPurple, modifier = Modifier.weight(1f))
                    ActionSquare(icon = Icons.Filled.Public, label = "Threat Map", color = DangerRed, modifier = Modifier.weight(1f), onClick = { navController.navigate(Screen.ThreatIntel.route) })
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("System Intelligence")
                DashboardSOC(guardianCore = guardianCore, modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ProtectionLogItem(text: String, isSuccess: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Filled.Check else Icons.Filled.Shield,
            contentDescription = null,
            tint = if (isSuccess) SafeGreen else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun StatItemSmall(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun AlertItem(threat: com.aegis.data.db.entity.ThreatEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, null, tint = DangerRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = threat.reason, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(text = "Source: ${threat.sourceApp ?: "System"}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSquare(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}
