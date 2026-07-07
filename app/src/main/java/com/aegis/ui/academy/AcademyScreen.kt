package com.aegis.ui.academy

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
import com.aegis.data.repository.LearningRepository
import com.aegis.ui.components.*
import com.aegis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyScreen(
    learningRepository: LearningRepository,
    guardianCore: GuardianCore,
    navController: androidx.navigation.NavController? = null,
    viewModel: AcademyViewModel = hiltViewModel()
) {
    val modules by viewModel.modules.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState()
    val totalScore by viewModel.totalScore.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Patriot Academy", fontWeight = FontWeight.Bold) },
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Completed",
                            value = "$completedCount/${modules.size}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        StatItem(
                            label = "Total Score",
                            value = "$totalScore",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        StatItem(
                            label = "Rank",
                            value = when {
                                completedCount == 0 -> "Recruit"
                                completedCount >= 8 -> "Patriot"
                                completedCount >= 5 -> "Guardian"
                                completedCount >= 3 -> "Defender"
                                else -> "Cadet"
                            },
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                SectionHeader("Special Practice")
                FeatureCard(
                    icon = { Icon(Icons.Filled.Psychology, null, tint = WarningOrange) },
                    title = "AI Scam Simulator",
                    description = "Test your awareness against real scenarios",
                    onClick = { navController?.navigate("scam_simulator") },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                SectionHeader("Learning Modules")
            }

            items(modules) { module ->
                val isCompleted = module.progress?.completed == true
                val moduleScore = module.progress?.score ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted)
                            SafeGreen.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    onClick = {
                        viewModel.completeModule(module.id, 100)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = module.icon,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = module.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isCompleted) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = SafeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = module.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = module.difficulty,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (module.difficulty) {
                                        "Beginner" -> SafeGreen
                                        "Intermediate" -> WarningYellow
                                        "Advanced" -> DangerRed
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${module.estimatedMinutes} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                if (isCompleted) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Score: $moduleScore%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafeGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
