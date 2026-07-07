package com.aegis.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.ai.GemmaInferenceEngine
import com.aegis.ui.theme.*

@Composable
fun ModelLoadingScreen(
    gemmaEngine: GemmaInferenceEngine,
    onLoadComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val isLoaded by gemmaEngine.isModelLoaded.collectAsState()
    val isLoading by gemmaEngine.isLoading.collectAsState()
    val progress by gemmaEngine.getLoadProgress().collectAsState()
    val error by gemmaEngine.getLoadError().collectAsState()
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    LaunchedEffect(isLoaded) {
        if (isLoaded) {
            kotlinx.coroutines.delay(500)
            onLoadComplete()
        }
    }
    
    LaunchedEffect(Unit) {
        if (!isLoaded && !isLoading) {
            gemmaEngine.loadModel()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0E14),
                        Color(0xFF1A1D24)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CyberBlue.copy(alpha = 0.3f),
                                CyberPurple.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = "AEGIS",
                    tint = CyberBlue,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(pulseScale)
                )
            }
            
            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "AEGIS",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberBlue,
                    letterSpacing = 4.sp
                )
                Text(
                    "Guardian Platform",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
            }
            
            // Loading Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1D24).copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        error != null -> {
                            // Error State
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = "Error",
                                tint = DangerRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Model Loading Failed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                            Text(
                                error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Button(
                                onClick = { gemmaEngine.loadModel() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberBlue
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                        isLoading -> {
                            // Loading State
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = CyberBlue,
                                strokeWidth = 4.dp
                            )
                            Text(
                                "Loading AI Model...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            // Progress Bar
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = CyberBlue,
                            )
                            
                            Text(
                                "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            
                            Text(
                                "Gemma 3N • ~1.5GB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        isLoaded -> {
                            // Success State
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Success",
                                tint = SafeGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "AI Model Ready",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen
                            )
                            Text(
                                "Gemma 3N loaded successfully",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // Skip Button
            if (isLoading) {
                TextButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Skip for now (use rule-based protection)")
                }
            }
        }
    }
}
