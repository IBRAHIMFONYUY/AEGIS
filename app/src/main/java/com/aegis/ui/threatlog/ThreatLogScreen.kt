package com.aegis.ui.threatlog

import androidx.compose.foundation.layout.*
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
                title = { Text("Threat Log", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientTopBar()

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

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (threats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No threats found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                ThreatTimeline(threats = threats)
            }
        }
    }
}
