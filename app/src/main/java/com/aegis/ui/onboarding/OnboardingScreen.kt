package com.aegis.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.ui.theme.*

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                title = "Welcome to AEGIS",
                description = "Your AI-powered Guardian Platform that protects your digital life from scams, fraud, and cyber threats in real-time.",
                icon = Icons.Filled.Security,
                color = CyberBlue
            ),
            OnboardingPage(
                title = "Real-Time Protection",
                description = "AEGIS monitors your messages, calls, and apps 24/7 using advanced AI to detect threats before they harm you.",
                icon = Icons.Filled.Shield,
                color = SafeGreen
            ),
            OnboardingPage(
                title = "Offline AI Security",
                description = "Powered by Gemma 3N, AEGIS works completely offline. Your data stays on your device - no cloud required.",
                icon = Icons.Filled.Psychology,
                color = CyberPurple
            ),
            OnboardingPage(
                title = "Guardian Score",
                description = "Track your digital safety with a live Guardian Score. Get personalized insights to improve your security posture.",
                icon = Icons.Filled.Analytics,
                color = ElectricPink
            )
        )
    }
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val scope = rememberCoroutineScope()
    
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundDarker,
                        BackgroundDark
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
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
                        .size(40.dp)
                        .scale(pulseScale)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "AEGIS",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = CyberBlue,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Page Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { index ->
                    val isSelected = index == currentPage
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 8.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "indicatorWidth_$index"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isSelected) pages[index].color else SurfaceDarkHigh,
                        animationSpec = tween(durationMillis = 300),
                        label = "indicatorColor_$index"
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            if (currentPage > 0) {
                                scope.launch {
                                    pagerState.animateScrollToPage(currentPage - 1)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = OnSurfaceDark
                        ),
                        shape = AegisButtonShape
                    ) {
                        Text("Back")
                    }
                }
                
                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.weight(if (currentPage > 0) 1f else 2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pages[currentPage].color
                    ),
                    shape = AegisButtonShape
                ) {
                    Text(
                        if (currentPage < pages.size - 1) "Next" else "Get Started",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(AegisCardShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            page.color.copy(alpha = 0.2f),
                            page.color.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                page.icon,
                contentDescription = null,
                tint = page.color,
                modifier = Modifier
                    .size(100.dp)
                    .offset(y = floatOffset.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceDark,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceDim,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
