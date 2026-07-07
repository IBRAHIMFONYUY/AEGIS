package com.aegis.ui.components

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
import com.aegis.core.AnalysisResult
import androidx.compose.ui.graphics.Color
import com.aegis.core.ThreatLevel
import com.aegis.core.toComposeColor
import com.aegis.data.db.entity.ThreatEvent
import com.aegis.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ThreatTimeline(
    threats: List<ThreatEvent>,
    modifier: Modifier = Modifier
) {
    if (threats.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = SafeGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No threats detected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        threats.forEach { threat ->
            ThreatTimelineItem(threat = threat)
        }
    }
}

@Composable
private fun ThreatTimelineItem(threat: ThreatEvent) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
    val level = ThreatLevel.entries.firstOrNull { it.value == threat.threatLevel } ?: ThreatLevel.SAFE
    val threatColor = Color(level.toComposeColor())
    val icon = when (level) {
        ThreatLevel.SAFE -> Icons.Filled.Shield
        ThreatLevel.SUSPICIOUS -> Icons.Filled.ReportProblem
        ThreatLevel.LIKELY_MALICIOUS -> Icons.Filled.Warning
        ThreatLevel.MALICIOUS -> Icons.Filled.Cancel
        ThreatLevel.CRITICAL -> Icons.Filled.Dangerous
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = threatColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = threat.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = threatColor
                    )
                    Text(
                        text = "Source: ${threat.sourceApp ?: "System"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                ThreatLevelIndicator(threatLevel = level)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (threat.sourceText.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Detected Content:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = threat.sourceText,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
            
            if (threat.suggestedAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = threatColor.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = threatColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AEGIS Advice: ${threat.suggestedAction}",
                            style = MaterialTheme.typography.bodySmall,
                            color = threatColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(threat.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
