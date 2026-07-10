package com.aegis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.core.GuardianScore
import com.aegis.core.ScoreCategory
import com.aegis.core.ScoreTrend
import com.aegis.ui.theme.*

@Composable
fun GuardianScoreCard(
    score: GuardianScore,
    modifier: Modifier = Modifier,
    onCategoryClick: ((ScoreCategory) -> Unit)? = null
) {
    val displayScore by animateFloatAsState(
        targetValue = score.overall * 100,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "scoreAnimation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AegisCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Guardian Score",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = "Real-time AI protection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberBlue
                    )
                }

                TrendIndicator(score.trend)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Score Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        val strokeWidth = 12.dp.toPx()
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = (size.width / 2) - strokeWidth / 2

                        // Background circle
                        drawCircle(
                            color = SurfaceDarkHigh,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )

                        // Progress arc
                        val sweepAngle = (displayScore / 100) * 360f
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    SafeGreen,
                                    CyberBlue,
                                    WarningOrange,
                                    DangerRed
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            size = Size(radius * 2, radius * 2),
                            topLeft = Offset(center.x - radius, center.y - radius)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${displayScore.toInt()}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = getScoreColor(score.overall)
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Category Scores
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                ) {
                    ScoreCategoryRow(
                        category = ScoreCategory.PRIVACY,
                        score = score.privacy,
                        onClick = onCategoryClick
                    )
                    ScoreCategoryRow(
                        category = ScoreCategory.SCAM_PROTECTION,
                        score = score.scamProtection,
                        onClick = onCategoryClick
                    )
                    ScoreCategoryRow(
                        category = ScoreCategory.DEVICE_SECURITY,
                        score = score.deviceSecurity,
                        onClick = onCategoryClick
                    )
                    ScoreCategoryRow(
                        category = ScoreCategory.DIGITAL_WELLBEING,
                        score = score.digitalWellbeing,
                        onClick = onCategoryClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreCategoryRow(
    category: ScoreCategory,
    score: Float,
    onClick: ((ScoreCategory) -> Unit)?
) {
    val animatedScore by animateFloatAsState(
        targetValue = score * 100,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "categoryScore_${category.name}"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AegisButtonShape)
            .background(SurfaceDarkHigh.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = category.icon,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceDark
            )
        }

        Text(
            text = "${animatedScore.toInt()}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = getScoreColor(score)
        )
    }
}

@Composable
private fun TrendIndicator(trend: ScoreTrend) {
    val (color, icon, text) = when (trend) {
        ScoreTrend.IMPROVING -> Triple(SafeGreen, "↑", "Improving")
        ScoreTrend.STABLE -> Triple(CyberBlue, "→", "Stable")
        ScoreTrend.DECLINING -> Triple(DangerRed, "↓", "Declining")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getScoreColor(score: Float): Color = when {
    score >= 0.8f -> SafeGreen
    score >= 0.6f -> CyberBlue
    score >= 0.4f -> WarningOrange
    else -> DangerRed
}
