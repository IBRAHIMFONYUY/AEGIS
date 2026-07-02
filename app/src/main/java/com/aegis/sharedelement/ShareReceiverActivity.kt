package com.aegis.sharedelement

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.AegisApplication
import com.aegis.core.*
import com.aegis.ui.components.ThreatLevelIndicator
import com.aegis.ui.theme.AegisTheme
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: run {
            finish()
            return
        }

        val app = application as AegisApplication
        val guardianCore = app.guardianCore

        setContent {
            AegisTheme {
                ShareAnalysisScreen(
                    text = sharedText,
                    guardianCore = guardianCore,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareAnalysisScreen(
    text: String,
    guardianCore: com.aegis.agents.GuardianCore,
    onDismiss: () -> Unit
) {
    var isAnalyzing by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<AnalysisResult?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val analysisResult = guardianCore.analyze(
            AnalysisContext(
                text = text,
                sourceType = SourceType.UNKNOWN,
                metadata = mapOf("source" to "share_extension")
            )
        )
        result = analysisResult
        isAnalyzing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AEGIS Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isAnalyzing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing content...")
                    }
                }
            } else {
                val analysisResult = result ?: return@Column

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Overall Assessment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ThreatLevelIndicator(
                            threatLevel = analysisResult.overallThreatLevel
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Agent Results",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                analysisResult.agentResults.forEach { agentResult ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = agentResult.agentName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                ThreatLevelIndicator(threatLevel = agentResult.threatLevel)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = agentResult.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            if (agentResult.confidence > 0) {
                                Text(
                                    text = "Confidence: ${(agentResult.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
