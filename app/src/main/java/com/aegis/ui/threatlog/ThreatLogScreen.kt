package com.aegis.ui.threatlog

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
import com.aegis.core.ThreatLevel
import com.aegis.data.repository.ThreatRepository
import com.aegis.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatLogScreen(
    threatRepository: ThreatRepository,
    viewModel: ThreatLogViewModel = hiltViewModel()
) {
    val filterLevel by viewModel.filterLevel.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val threats = viewModel.getFilteredThreats()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Threat Intelligence", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientTopBar()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    ThreatMapComponent(modifier = Modifier.padding(16.dp))
                }

                item {
                    SectionHeader("Intelligence Filters")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThreatLevel.entries.forEach { level ->
                            FilterChip(
                                selected = filterLevel == level,
                                onClick = { viewModel.setFilterLevel(level) },
                                label = { Text(level.label, style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (level) {
                                            ThreatLevel.SAFE -> Icons.Filled.Shield
                                            ThreatLevel.SUSPICIOUS -> Icons.Filled.ReportProblem
                                            ThreatLevel.LIKELY_MALICIOUS -> Icons.Filled.Warning
                                            ThreatLevel.MALICIOUS -> Icons.Filled.Cancel
                                            ThreatLevel.CRITICAL -> Icons.Filled.Dangerous
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (threats.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No threats in local intelligence",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                } else {
                    item {
                        SectionHeader("Recent Incidents")
                    }
                    items(threats) { threat ->
                        ThreatTimelineItem(threat = threat)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreatTimelineItem(threat: com.aegis.data.db.entity.ThreatEvent) {
    // Reusing the existing component logic but inside this file or importing it
    ThreatTimeline(threats = listOf(threat))
}
