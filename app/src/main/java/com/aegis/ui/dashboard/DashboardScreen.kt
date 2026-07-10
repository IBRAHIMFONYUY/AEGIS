package com.aegis.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aegis.agents.GuardianCore
import com.aegis.core.ScoreCategory
import com.aegis.data.repository.SafetyRepository
import com.aegis.data.repository.ThreatRepository
import com.aegis.ui.components.*
import com.aegis.ui.navigation.Screen
import com.aegis.ui.theme.*

/** Simple spacing scale so every section agrees on rhythm instead of ad-hoc dp values. */
private object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Breakpoint above which we treat the surface as a tablet / foldable-unfolded layout. */
private val TABLET_BREAKPOINT = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    guardianCore: GuardianCore,
    safetyRepository: SafetyRepository,
    threatRepository: ThreatRepository,
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {

    val safetyScoreData by viewModel.safetyScore.collectAsState()
    val guardianScore by guardianCore.guardianScore.collectAsState()
    val overallScore by remember(guardianScore) {
        derivedStateOf { guardianScore.overall }
    }

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val protectionStats by viewModel.protectionStats.collectAsState()
    val recentThreats by viewModel.recentThreats.collectAsState()

    // Real-time analysis results
    val lastAnalysisResult by viewModel.lastResult.collectAsState()

    // Single source of truth from the ViewModel — no more re-deriving the threshold in the UI.
    val isProtected by viewModel.isProtected.collectAsState()
    val greeting by viewModel.greeting.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(scanError) {
        scanError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeScanError()
        }
    }

    val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "$greeting, $userName",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(active = isProtected)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AEGIS is watching your back",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberBlue
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.semantics { contentDescription = "Open profile and settings" }
                    ) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = CyberBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        val screenWidthDp = LocalContext.current.resources.configuration.screenWidthDp.dp
        val isTablet = screenWidthDp >= TABLET_BREAKPOINT
        val horizontalInset = if (isTablet) Space.xxl else Space.lg
        val contentMaxWidth = if (isTablet) 720.dp else screenWidthDp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isTablet) Modifier.widthIn(max = contentMaxWidth) else Modifier.fillMaxWidth())
                    .align(Alignment.TopCenter),
                horizontalAlignment = if (isTablet) Alignment.CenterHorizontally else Alignment.Start,
                contentPadding = PaddingValues(bottom = Space.xxl + Space.xxl)
            ) {

                item { GradientTopBar() }

                item {
                    HeroScoreSection(
                        overallScore = overallScore,
                        isProtected = isProtected,
                        isAnalyzing = isAnalyzing,
                        secondaryTextColor = secondaryTextColor,
                        horizontalInset = horizontalInset,
                        guardianScore = guardianScore,
                        onCategoryClick = { category ->
                            when (category) {
                                ScoreCategory.PRIVACY -> navController.navigate(Screen.Privacy.route)
                                ScoreCategory.SCAM_PROTECTION -> navController.navigate(Screen.ThreatIntel.route)
                                ScoreCategory.DEVICE_SECURITY -> navController.navigate(Screen.Settings.route)
                                ScoreCategory.DIGITAL_WELLBEING -> navController.navigate(Screen.Academy.route)
                            }
                        },
                        onScanClick = { viewModel.runFullScan() }
                    )
                }

                item {
                    SectionHeader("Today's Protection")

                    AnimatedVisibility(
                        visible = isAnalyzing,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = horizontalInset)) {
                            ProtectionLogItem(
                                text = "Deep AI reasoning in progress\u2026",
                                isSuccess = false,
                                isPulsing = true
                            )
                            Spacer(modifier = Modifier.height(Space.sm))
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier
                            .padding(horizontal = horizontalInset)
                            .fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                        shape = AegisCardShape
                    ) {
                        Column(modifier = Modifier.padding(Space.lg)) {
                            ProtectionLogItem("${protectionStats.scamsBlocked} scam messages blocked", true)
                            ProtectionLogItem("${protectionStats.linksBlocked} dangerous links blocked", true)
                            ProtectionLogItem("${protectionStats.fakeNewsDetected} fake news items detected", true)
                            ProtectionLogItem("Microphone secured", protectionStats.micSecured)
                            ProtectionLogItem("Camera secured", protectionStats.cameraSecured)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Space.xl))
                    SectionHeader("Recent Alerts")
                }

                if (recentThreats.isNotEmpty()) {
                    items(
                        items = recentThreats,
                        key = { it.id }
                    ) { threat ->
                        AlertItem(threat, horizontalInset = horizontalInset)
                    }
                } else {
                    item {
                        AllClearCard(horizontalInset = horizontalInset)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Space.xl))
                    SectionHeader("Quick Actions")
                    QuickActionsGrid(
                        isTablet = isTablet,
                        horizontalInset = horizontalInset,
                        navController = navController
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Space.xl))
                    SectionHeader("System Intelligence")
                    DashboardSOC(
                        guardianCore = guardianCore,
                        modifier = Modifier.padding(horizontal = horizontalInset)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(Space.xxl))
                }
            }
        }
    }
}

/**
 * Hero section: headline question, the animated Guardian score ring, and the primary
 * scan CTA. Pulled out of the main composable so the score math and layout stay legible.
 */
@Composable
private fun HeroScoreSection(
    overallScore: Float,
    isProtected: Boolean,
    isAnalyzing: Boolean,
    secondaryTextColor: Color,
    horizontalInset: androidx.compose.ui.unit.Dp,
    guardianScore: Any, // typed as the real GuardianScore model in your codebase
    onCategoryClick: (ScoreCategory) -> Unit,
    onScanClick: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = if (isProtected) SafeGreen else WarningOrange,
        animationSpec = tween(400),
        label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        statusColor.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = horizontalInset, vertical = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Am I safe right now?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Light,
            color = secondaryTextColor
        )

        Spacer(modifier = Modifier.height(Space.xl))

        // GuardianScoreCard is assumed to accept the same signature as before; wrapping it
        // in a subtly bordered container ties it visually to the status color above.
        Box(
            modifier = Modifier
                .clip(AegisCardShape)
                .border(1.dp, statusColor.copy(alpha = 0.25f), AegisCardShape)
        ) {
            @Suppress("UNCHECKED_CAST")
            GuardianScoreCard(
                score = guardianScore as com.aegis.core.GuardianScore,
                onCategoryClick = onCategoryClick
            )
        }

        Spacer(modifier = Modifier.height(Space.xl))

        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth(if (horizontalInset > Space.lg) 0.5f else 0.8f)
                .heightIn(min = 52.dp),
            enabled = !isAnalyzing,
            shape = AegisButtonShape,
            contentPadding = PaddingValues(vertical = Space.md),
            colors = ButtonDefaults.buttonColors(
                containerColor = AegisPrimary,
                contentColor = Color.Black
            )
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Filled.Security, contentDescription = null)
            }

            Spacer(modifier = Modifier.width(Space.sm))

            Text(
                text = if (isAnalyzing) "Analyzing\u2026" else "Run Guardian Scan",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Small live/idle indicator dot next to the tagline in the top bar. */
@Composable
private fun StatusDot(active: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusAlpha"
    )
    val color = if (active) SafeGreen else WarningOrange
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color.copy(alpha = if (active) alpha else 1f), androidx.compose.foundation.shape.CircleShape)
    )
}

@Composable
fun ProtectionLogItem(text: String, isSuccess: Boolean, isPulsing: Boolean = false) {
    val iconTint = if (isSuccess) SafeGreen else MaterialTheme.colorScheme.primary
    val alpha = if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "logPulse")
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logAlpha"
        ).value
    } else 1f

    Row(
        modifier = Modifier
            .padding(vertical = Space.xs)
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Filled.Check else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = iconTint.copy(alpha = alpha),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(Space.md))

        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
    }
}

@Composable
fun StatItemSmall(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AlertItem(
    threat: com.aegis.data.db.entity.ThreatEvent,
    horizontalInset: androidx.compose.ui.unit.Dp = Space.lg
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalInset, vertical = Space.xs),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        ),
        shape = AegisCardShape
    ) {
        Row(
            modifier = Modifier.padding(Space.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = "Threat alert",
                tint = DangerRed,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(Space.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = threat.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Source: ${threat.sourceApp ?: "System"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** Reassuring empty state shown instead of silently hiding the Recent Alerts section. */
@Composable
private fun AllClearCard(horizontalInset: androidx.compose.ui.unit.Dp) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalInset, vertical = Space.xs),
        colors = CardDefaults.cardColors(
            containerColor = SafeGreen.copy(alpha = 0.10f)
        ),
        shape = AegisCardShape
    ) {
        Row(
            modifier = Modifier.padding(Space.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = SafeGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(Space.md))
            Column {
                Text(
                    text = "All clear",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No threats detected recently. AEGIS is monitoring in the background.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Quick actions laid out as two rows of three on phones, and a single row of six on
 * tablets/unfolded foldables, so the grid actually uses the extra width instead of
 * stretching three squares across a large screen.
 */
@Composable
private fun QuickActionsGrid(
    isTablet: Boolean,
    horizontalInset: androidx.compose.ui.unit.Dp,
    navController: NavController
) {
    data class Action(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val label: String,
        val color: Color,
        val onClick: () -> Unit
    )

    val actions = listOf(
        Action(Icons.Filled.Shield, "Shield", SafeGreen) { },
        Action(Icons.Filled.Https, "Vault", WarningOrange) { navController.navigate(Screen.Vault.route) },
        Action(Icons.Filled.SmartToy, "AI Guardian", CyberBlue) { navController.navigate(Screen.Assistant.route) },
        Action(Icons.Filled.Lock, "Privacy", CyberPurple) { navController.navigate(Screen.Privacy.route) },
        Action(Icons.Filled.Public, "Threat Map", DangerRed) { navController.navigate(Screen.ThreatIntel.route) },
        Action(Icons.Filled.School, "Academy", WarningYellow) { navController.navigate(Screen.Academy.route) }
    )

    if (isTablet) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalInset),
            horizontalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            actions.forEach { action ->
                ActionSquare(
                    icon = action.icon,
                    label = action.label,
                    color = action.color,
                    modifier = Modifier.weight(1f),
                    onClick = action.onClick
                )
            }
        }
    } else {
        val rows = actions.chunked(3)
        Column {
            rows.forEachIndexed { index, rowActions ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalInset),
                    horizontalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    rowActions.forEach { action ->
                        ActionSquare(
                            icon = action.icon,
                            label = action.label,
                            color = action.color,
                            modifier = Modifier.weight(1f),
                            onClick = action.onClick
                        )
                    }
                }
                if (index != rows.lastIndex) {
                    Spacer(modifier = Modifier.height(Space.md))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSquare(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = label },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = AegisCardShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(Space.sm))
            Text(
                label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}