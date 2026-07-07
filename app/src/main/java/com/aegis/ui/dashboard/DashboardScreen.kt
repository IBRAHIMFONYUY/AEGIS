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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aegis.agents.GuardianCore
import com.aegis.core.ScoreCategory
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.ThreatRepository
import com.aegis.ui.components.*
import com.aegis.ui.navigation.Screen
import com.aegis.ui.theme.*
import java.util.*

private const val SAFE_THRESHOLD = 0.8f

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
    val guardianScore by guardianCore.guardianScore.collectAsState()
    val overallScore by remember(guardianScore) {
        derivedStateOf { guardianScore.overall }
    }

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val protectionStats by viewModel.protectionStats.collectAsState()
    val recentThreats by viewModel.recentThreats.collectAsState()

    val isProtected = overallScore >= SAFE_THRESHOLD

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$greeting, $userName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Text(
                            text = "AEGIS is watching your back",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberBlue
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Profile",
                            tint = CyberBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = BackgroundDark
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            item { GradientTopBar() }

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
                        color = secondaryTextColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    GuardianScoreCard(
                        score = guardianScore,
                        onCategoryClick = { category ->
                            // Navigate to detailed category view
                            when (category) {
                                ScoreCategory.PRIVACY -> navController.navigate(Screen.Privacy.route)
                                ScoreCategory.SCAM_PROTECTION -> navController.navigate(Screen.ThreatIntel.route)
                                ScoreCategory.DEVICE_SECURITY -> navController.navigate(Screen.Settings.route)
                                ScoreCategory.DIGITAL_WELLBEING -> navController.navigate(Screen.Academy.route)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.runFullScan() },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        enabled = !isAnalyzing,
                        shape = MaterialTheme.shapes.extraLarge,
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Security, contentDescription = "Scan")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isAnalyzing)
                                "Analyzing..."
                            else
                                "Run Guardian Scan",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                SectionHeader("Today's Protection")

                if (isAnalyzing) {
                    ProtectionLogItem(
                        text = "Deep AI reasoning in progress...",
                        isSuccess = false
                    )
                }

                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProtectionLogItem("${protectionStats.scamsBlocked} Scam messages blocked", true)
                        ProtectionLogItem("${protectionStats.linksBlocked} Dangerous links blocked", true)
                        ProtectionLogItem("${protectionStats.fakeNewsDetected} Fake news detected", true)
                        ProtectionLogItem("Microphone secured", protectionStats.micSecured)
                        ProtectionLogItem("Camera secured", protectionStats.cameraSecured)
                    }
                }
            }

            if (recentThreats.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionHeader("Recent Alerts")
                }

                items(
                    items = recentThreats,
                    key = { it.id }
                ) { threat ->
                    AlertItem(threat)
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
                    ActionSquare(
                        icon = Icons.Filled.Shield,
                        label = "Shield",
                        color = SafeGreen,
                        modifier = Modifier.weight(1f)
                    )

                    ActionSquare(
                        icon = Icons.Filled.Https,
                        label = "Vault",
                        color = WarningOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Vault.route) }
                    )

                    ActionSquare(
                        icon = Icons.Filled.SmartToy,
                        label = "AI Guardian",
                        color = CyberBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Assistant.route) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionSquare(
                        icon = Icons.Filled.Lock,
                        label = "Privacy",
                        color = CyberPurple,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Privacy.route) }
                    )

                    ActionSquare(
                        icon = Icons.Filled.Public,
                        label = "Threat Map",
                        color = DangerRed,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.ThreatIntel.route) }
                    )

                    ActionSquare(
                        icon = Icons.Filled.School,
                        label = "Academy",
                        color = WarningYellow,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Academy.route) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("System Intelligence")
                DashboardSOC(
                    guardianCore = guardianCore,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
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
            imageVector = if (isSuccess)
                Icons.Filled.Check
            else
                Icons.Filled.ErrorOutline,
            contentDescription = "Status",
            tint = if (isSuccess) SafeGreen else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun StatItemSmall(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AlertItem(threat: com.aegis.data.db.entity.ThreatEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = "Threat",
                tint = DangerRed,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = threat.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Source: ${threat.sourceApp ?: "System"}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSquare(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
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
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}