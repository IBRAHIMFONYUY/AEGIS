package com.aegis.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale

@Composable
fun AnimatedCard(
    visible: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it / 2 },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    ) {
        Card(
            modifier = modifier,
            shape = shape,
            elevation = elevation,
            colors = colors
        ) {
            content()
        }
    }
}

@Composable
fun ScaleInCard(
    visible: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            transformOrigin = TransformOrigin.Center
        ) + fadeIn(
            animationSpec = tween(durationMillis = 300)
        ),
        exit = scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(durationMillis = 300),
            transformOrigin = TransformOrigin.Center
        ) + fadeOut(
            animationSpec = tween(durationMillis = 300)
        )
    ) {
        Card(
            modifier = modifier,
            shape = shape,
            elevation = elevation,
            colors = colors
        ) {
            content()
        }
    }
}

@Composable
fun ShimmerLoadingCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    contentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha: Float by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = contentColor.copy(alpha = alpha)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
    }
}

@Composable
fun PulseCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors: CardColors = CardDefaults.cardColors(),
    isPulsing: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale: Float by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val elevationValue by infiniteTransition.animateValue(
        initialValue = 4.dp,
        targetValue = 8.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "elevation"
    )
    
    Card(
        modifier = modifier.then(
            if (isPulsing) Modifier.scale(scale) else Modifier
        ),
        shape = shape,
        elevation = if (isPulsing) {
            CardDefaults.cardElevation(defaultElevation = elevationValue)
        } else {
            elevation
        },
        colors = colors
    ) {
        content()
    }
}

@Composable
fun StaggeredGrid(
    items: List<Any>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Any) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEachIndexed { index, item ->
            val delay = index * 100L
            key(index) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(
                            durationMillis = 400,
                            delayMillis = delay.toInt(),
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 400,
                            delayMillis = delay.toInt()
                        )
                    )
                ) {
                    itemContent(item)
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealCard(
    modifier: Modifier = Modifier,
    onReveal: () -> Unit,
    content: @Composable () -> Unit
) {
    // Simplified swipe-to-reveal implementation
    // In a full implementation, this would use swipe gestures
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        content()
    }
}

@Composable
fun BounceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val scale: Float by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                1f at 0 with LinearEasing
                1.05f at 100 with LinearEasing
                1f at 200 with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "bounce"
    )
    
    Button(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled
    ) {
        content()
    }
}

@Composable
fun RotatingIcon(
    isRotating: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation: Float by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier.rotate(if (isRotating) rotation else 0f)
    ) {
        content()
    }
}
