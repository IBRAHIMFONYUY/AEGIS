package com.aegis.ui.threatlog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.core.ThreatLevel
import com.aegis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatDetailScreen(
    threatId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ThreatDetailViewModel = hiltViewModel()
) {
    val threat by viewModel.threat.collectAsState()
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    LaunchedEffect(threatId) {
        viewModel.loadThreat(threatId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Audit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val currentThreat = threat
        if (currentThreat == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AegisPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                val color = when (currentThreat.threatLevel) {
                    ThreatLevel.CRITICAL.value -> DangerRed
                    ThreatLevel.MALICIOUS.value -> DangerRed
                    ThreatLevel.LIKELY_MALICIOUS.value -> WarningOrange
                    else -> WarningYellow
                }

                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (currentThreat.threatLevel >= ThreatLevel.MALICIOUS.value) Icons.Filled.GppBad else Icons.Filled.Security,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "CRITICAL THREAT DETECTED",
                                style = MaterialTheme.typography.titleMedium,
                                color = color,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Source: ${currentThreat.sourceApp?.split('.')?.lastOrNull() ?: "Unknown App"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Detected at: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(currentThreat.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("THREAT CONTENT", style = MaterialTheme.typography.labelLarge, color = AegisPrimary, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = currentThreat.sourceText,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Guardian Evaluation
                Text("GUARDIAN EVALUATION", style = MaterialTheme.typography.labelLarge, color = AegisPrimary, fontWeight = FontWeight.Bold)
                Text(
                    text = currentThreat.reason,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Additional Details
                if (!currentThreat.details.isNullOrBlank()) {
                    Text("TECHINAL SIGNALS", style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = currentThreat.details,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )
                    }
                }

                if (currentThreat.suggestedAction != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("RECOMMENDED ACTIONS", style = MaterialTheme.typography.labelLarge, color = SafeGreen, fontWeight = FontWeight.Bold)
                    Text(
                        text = currentThreat.suggestedAction,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // AI Audit Section
                if (aiAnalysis == null) {
                    Button(
                        onClick = { viewModel.analyzeWithGemini() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AegisPrimary),
                        enabled = !isAnalyzing
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RUN DEEP AI AUDIT (GEMINI)", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    Text("DEEP AI AUDIT REPORT", style = MaterialTheme.typography.labelLarge, color = AegisPrimary, fontWeight = FontWeight.Bold)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = AegisPrimary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AegisPrimary)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.SmartToy, null, tint = AegisPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gemini 3.5 Flash Evaluation", fontWeight = FontWeight.Bold, color = AegisPrimary)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = aiAnalysis!!,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
