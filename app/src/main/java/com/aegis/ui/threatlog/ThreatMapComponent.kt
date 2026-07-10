package com.aegis.ui.threatlog

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

data class CameroonRegion(
    val name: String,
    val x: Float,
    val y: Float,
    val population: Int
)

val cameroonRegions = listOf(
    CameroonRegion("Douala", 0.15f, 0.75f, 3500000),
    CameroonRegion("Yaoundé", 0.35f, 0.55f, 2800000),
    CameroonRegion("Bamenda", 0.25f, 0.25f, 500000),
    CameroonRegion("Buea", 0.12f, 0.82f, 300000),
    CameroonRegion("Garoua", 0.65f, 0.20f, 600000),
    CameroonRegion("Maroua", 0.75f, 0.15f, 500000),
    CameroonRegion("Ngaoundéré", 0.55f, 0.35f, 400000),
    CameroonRegion("Bafoussam", 0.22f, 0.45f, 350000),
    CameroonRegion("Kribi", 0.18f, 0.88f, 200000),
    CameroonRegion("Ebolowa", 0.30f, 0.80f, 150000)
)

data class ThreatIncident(
    val id: String,
    val region: String,
    val x: Float,
    val y: Float,
    val threatType: ThreatType,
    val severity: Float,
    val timestamp: Long,
    val description: String
)

enum class ThreatType(val displayName: String, val color: Color) {
    PHISHING("Phishing", Color(0xFFFF6B6B)),
    SCAM("Scam", Color(0xFFFFA500)),
    MALWARE("Malware", Color(0xFF9B59B6)),
    MOMO_FRAUD("MoMo Fraud", Color(0xFF3498DB)),
    IDENTITY_THEFT("Identity Theft", Color(0xFFE74C3C)),
    DEEPFAKE("Deepfake", Color(0xFF1ABC9C))
}

@Composable
fun ThreatMapComponent(modifier: Modifier = Modifier) {
    var activeThreats by remember { mutableStateOf(listOf<ThreatIncident>()) }
    var selectedThreat by remember { mutableStateOf<ThreatIncident?>(null) }
    var totalIncidents by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            // Generate realistic threat incidents across Cameroon
            val newThreats = cameroonRegions.mapNotNull { region ->
                if (Random.nextFloat() > 0.3f) {
                    val threatType = ThreatType.entries.random()
                    ThreatIncident(
                        id = "THREAT-${System.currentTimeMillis()}-${Random.nextInt()}",
                        region = region.name,
                        x = region.x + (Random.nextFloat() - 0.5f) * 0.08f,
                        y = region.y + (Random.nextFloat() - 0.5f) * 0.08f,
                        threatType = threatType,
                        severity = Random.nextFloat(),
                        timestamp = System.currentTimeMillis(),
                        description = when (threatType) {
                            ThreatType.PHISHING -> "Suspicious banking message detected"
                            ThreatType.SCAM -> "Lottery scam attempt reported"
                            ThreatType.MALWARE -> "Malicious app installation blocked"
                            ThreatType.MOMO_FRAUD -> "Unauthorized MoMo transaction attempt"
                            ThreatType.IDENTITY_THEFT -> "Personal data harvesting attempt"
                            ThreatType.DEEPFAKE -> "Synthetic media content detected"
                        }
                    )
                } else null
            }
            
            activeThreats = newThreats
            totalIncidents += newThreats.size
            delay(3000)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp),
        shape = AegisCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0E14))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Map Background with Cameroon outline approximation
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Grid
                val gridColor = Color.White.copy(alpha = 0.03f)
                val step = 50.dp.toPx()
                for (x in 0..size.width.toInt() step step.toInt()) {
                    drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
                }
                for (y in 0..size.height.toInt() step step.toInt()) {
                    drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
                }

                // Cameroon regions (simplified visualization)
                cameroonRegions.forEach { region ->
                    val center = Offset(size.width * region.x, size.height * region.y)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = (region.population / 500000f).coerceIn(2f, 8f).dp.toPx(),
                        center = center
                    )
                }
            }

            // Threat points
            activeThreats.forEach { threat ->
                ThreatPulse(
                    threat = threat,
                    onClick = { selectedThreat = threat }
                )
            }

            // Header
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LIVE THREAT MAP",
                        style = MaterialTheme.typography.titleMedium,
                        color = SafeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Cameroon Cyber-Incident Tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Total Incidents Today: $totalIncidents",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            // Legend
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .clip(AegisButtonShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text(
                    "Threat Types",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThreatType.entries.take(4).forEach { type ->
                    LegendItem(type)
                }
            }

            // Live indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(DangerRed.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pulseScale by rememberInfiniteTransition(label = "livePulse").animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "livePulse"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LIVE",
                        color = DangerRed,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Selected threat detail
            selectedThreat?.let { threat ->
                ThreatDetailCard(
                    threat = threat,
                    onDismiss = { selectedThreat = null },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

@Composable
private fun ThreatPulse(
    threat: ThreatIncident,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * threat.x, size.height * threat.y)
            val radius = (4.dp.toPx() + threat.severity * 6.dp.toPx())
            
            // Core
            drawCircle(
                color = threat.threatType.color,
                radius = radius,
                center = center
            )
            
            // Pulse rings
            drawCircle(
                color = threat.threatType.color.copy(alpha = alpha),
                radius = (radius + 20.dp.toPx() * scale),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = threat.threatType.color.copy(alpha = alpha * 0.5f),
                radius = (radius + 35.dp.toPx() * scale),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun LegendItem(type: ThreatType) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(type.color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            type.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ThreatDetailCard(
    threat: ThreatIncident,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .width(280.dp),
        shape = AegisCardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1D24)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = threat.threatType.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        threat.threatType.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = threat.threatType.color
                    )
                }
                
                TextButton(onClick = onDismiss) {
                    Text("✕", color = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                threat.region,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                threat.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Severity: ${(threat.severity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        threat.severity >= 0.7f -> DangerRed
                        threat.severity >= 0.4f -> WarningOrange
                        else -> SafeGreen
                    }
                )
                Text(
                    "Just now",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}
