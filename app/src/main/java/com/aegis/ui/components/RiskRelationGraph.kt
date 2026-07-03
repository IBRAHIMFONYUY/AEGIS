package com.aegis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.core.ThreatLevel
import com.aegis.data.db.entity.ThreatEvent
import com.aegis.ui.theme.*

@Composable
fun RiskRelationGraph(
    threat: ThreatEvent,
    modifier: Modifier = Modifier
) {
    val level = ThreatLevel.entries.firstOrNull { it.value == threat.threatLevel } ?: ThreatLevel.SAFE
    val color = when (level) {
        ThreatLevel.SAFE -> SafeGreen
        ThreatLevel.SUSPICIOUS -> WarningYellow
        ThreatLevel.LIKELY_MALICIOUS -> WarningOrange
        else -> DangerRed
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw lines from center to outer nodes
            val nodeCount = 5
            val radius = size.height * 0.35f
            
            for (i in 0 until nodeCount) {
                val angle = (i * (360f / nodeCount)) * (Math.PI / 180f).toFloat()
                val target = Offset(
                    center.x + Math.cos(angle.toDouble()).toFloat() * radius,
                    center.y + Math.sin(angle.toDouble()).toFloat() * radius
                )
                drawLine(
                    color = color.copy(alpha = 0.4f),
                    start = center,
                    end = target,
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Center Node: The Threat
        GraphNode(
            icon = Icons.Filled.Warning,
            label = "THREAT",
            color = color,
            modifier = Modifier.align(Alignment.Center)
        )

        // Outer Nodes
        Box(modifier = Modifier.fillMaxSize()) {
            // App Node
            GraphNode(
                icon = Icons.Filled.Apps,
                label = threat.sourceApp?.split('.')?.lastOrNull() ?: "App",
                color = CyberBlue,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = 10.dp)
            )
            
            // Source Node
            GraphNode(
                icon = Icons.Filled.Share,
                label = threat.sourceType,
                color = CyberPurple,
                modifier = Modifier.align(Alignment.CenterStart).offset(x = 10.dp)
            )
            
            // Agent Node
            GraphNode(
                icon = Icons.Filled.Security,
                label = threat.agentName,
                color = SafeGreen,
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-10).dp)
            )
            
            // Risk Node
            GraphNode(
                icon = Icons.Filled.BarChart,
                label = "${(threat.confidence * 100).toInt()}% Risk",
                color = WarningOrange,
                modifier = Modifier.align(Alignment.BottomStart).offset(x = 30.dp, y = (-20).dp)
            )
            
            // Action Node
            GraphNode(
                icon = if (threat.isResolved) Icons.Filled.CheckCircle else Icons.Filled.History,
                label = if (threat.isResolved) "Resolved" else "Pending",
                color = if (threat.isResolved) SafeGreen else MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = (-20).dp)
            )
        }
    }
}

@Composable
private fun GraphNode(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, color),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
