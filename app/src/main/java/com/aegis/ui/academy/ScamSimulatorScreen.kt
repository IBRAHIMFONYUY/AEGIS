package com.aegis.ui.academy

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.ui.components.GradientTopBar
import com.aegis.ui.theme.*

enum class Difficulty(val displayName: String, val multiplier: Float) {
    BEGINNER("Beginner", 1.0f),
    INTERMEDIATE("Intermediate", 1.5f),
    ADVANCED("Advanced", 2.0f)
}

enum class ScamType(val displayName: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PHISHING("Phishing", Icons.Filled.Phishing),
    JOB_SCAM("Job Scam", Icons.Filled.Work),
    FINANCIAL("Financial Fraud", Icons.Filled.AccountBalanceWallet),
    ROMANCE("Romance Scam", Icons.Filled.Favorite),
    IMPERSONATION("Impersonation", Icons.Filled.Person),
    TECH_SUPPORT("Tech Support", Icons.Filled.SupportAgent),
    LOTTERY("Lottery/Prize", Icons.Filled.EmojiEvents),
    URGENT_ACTION("Urgent Action", Icons.Filled.Timer)
}

data class Scenario(
    val id: String,
    val title: String,
    val content: String,
    val isScam: Boolean,
    val scamType: ScamType,
    val difficulty: Difficulty,
    val explanation: String,
    val redFlags: List<String>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScamSimulatorScreen(onComplete: (Int) -> Unit) {
    var selectedDifficulty by remember { mutableStateOf(Difficulty.BEGINNER) }
    var currentStep by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var showExplanation by remember { mutableStateOf(false) }
    var lastAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    
    val scenarios = remember(selectedDifficulty) { getScenarios(selectedDifficulty) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AI Scam Simulator", fontWeight = FontWeight.Bold)
                        Text(
                            "Practice against real-world scams",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    DifficultySelector(
                        selected = selectedDifficulty,
                        onSelected = { 
                            selectedDifficulty = it
                            currentStep = 0
                            score = 0
                            showExplanation = false
                        }
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GradientTopBar()
            
            if (currentStep == 0 && !showExplanation) {
                // Welcome screen
                WelcomeScreen(
                    difficulty = selectedDifficulty,
                    onStart = { currentStep = 1 }
                )
            } else if (currentStep <= scenarios.size) {
                val scenarioIndex = currentStep - 1
                if (scenarioIndex >= 0 && scenarioIndex < scenarios.size) {
                    val scenario = scenarios[scenarioIndex]
                    
                    LinearProgressIndicator(
                        progress = currentStep.toFloat() / (scenarios.size + 1),
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberBlue
                    )
                    
                    ScenarioCard(
                        scenario = scenario,
                        step = currentStep,
                        total = scenarios.size,
                        showExplanation = showExplanation,
                        lastAnswerCorrect = lastAnswerCorrect,
                        onAnswer = { isScam ->
                            val correct = (isScam == scenario.isScam)
                            lastAnswerCorrect = correct
                            if (correct) {
                                score += (20 * scenario.difficulty.multiplier).toInt()
                            }
                            showExplanation = true
                        },
                        onNext = {
                            showExplanation = false
                            currentStep++
                        }
                    )
                }
            } else {
                SimulationResults(
                    score = score,
                    difficulty = selectedDifficulty,
                    onFinish = { onComplete(score) }
                )
            }
        }
    }
}

@Composable
private fun DifficultySelector(
    selected: Difficulty,
    onSelected: (Difficulty) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(selected.displayName)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Difficulty.entries.forEach { difficulty ->
                DropdownMenuItem(
                    text = { Text(difficulty.displayName) },
                    onClick = {
                        onSelected(difficulty)
                        expanded = false
                    },
                    leadingIcon = {
                        if (difficulty == selected) {
                            Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    difficulty: Difficulty,
    onStart: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = CyberBlue
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Welcome to Scam Simulator",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Difficulty: ${difficulty.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = AegisCardShape
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "How it works:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BulletPoint("You'll see realistic scam scenarios")
                    BulletPoint("Decide if it's a scam or legitimate")
                    BulletPoint("Learn the red flags and explanations")
                    BulletPoint("Build your digital intuition")
                }
            }
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = WarningOrange.copy(alpha = 0.2f)
                ),
                shape = AegisCardShape
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = WarningOrange)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Cameroon-Specific Scams",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    BulletPoint("MoMo fraud attempts")
                    BulletPoint("Fake job offers")
                    BulletPoint("Banking phishing")
                    BulletPoint("Government impersonation")
                }
            }
        }
        
        item {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                shape = AegisButtonShape,
                contentPadding = PaddingValues(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimary, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Training", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = SafeGreen)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ScenarioCard(
    scenario: Scenario,
    step: Int,
    total: Int,
    showExplanation: Boolean,
    lastAnswerCorrect: Boolean?,
    onAnswer: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Scenario $step of $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        scenario.scamType.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        scenario.scamType.displayName,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = AegisCardShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    scenario.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            scenario.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        scenario.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified // Default
                    )
                }
            }
        }
        
        if (!showExplanation) {
            item {
                Column {
                    Text(
                        "Is this a scam?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { 
                                if (scenario.isScam) {
                                    onAnswer(true)
                                } else {
                                    onAnswer(false)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarningOrange,
                                contentColor = Color.White
                            ),
                            shape = AegisButtonShape,
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SCAM")
                        }
                        Button(
                            onClick = { 
                                if (!scenario.isScam) {
                                    onAnswer(true)
                                } else {
                                    onAnswer(false)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SafeGreen,
                                contentColor = Color.White
                            ),
                            shape = AegisButtonShape,
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SAFE")
                        }
                    }
                }
            }
        } else {
            item {
                val resultColor = if (lastAnswerCorrect == true) SafeGreen else DangerRed
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = resultColor.copy(alpha = 0.1f)
                    ),
                    shape = AegisCardShape
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (lastAnswerCorrect == true) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = null,
                                tint = resultColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (lastAnswerCorrect == true) "Correct!" else "Incorrect",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = resultColor
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            scenario.explanation,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Red Flags:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        scenario.redFlags.forEach { flag ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Filled.Circle, null, modifier = Modifier.size(6.dp), tint = resultColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(flag, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            
            item {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AegisButtonShape,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AegisPrimary, contentColor = Color.Black)
                ) {
                    Text(if (step < total) "Next Scenario" else "See Results", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SimulationResults(
    score: Int,
    difficulty: Difficulty,
    onFinish: () -> Unit
) {
    val maxScore = (20 * 5 * difficulty.multiplier).toInt()
    val percentage = (score.toFloat() / maxScore * 100).toInt()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val scale by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        
        Icon(
            imageVector = when {
                percentage >= 80 -> Icons.Filled.VerifiedUser
                percentage >= 60 -> Icons.Filled.ThumbUp
                else -> Icons.Filled.School
            },
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = when {
                percentage >= 80 -> SafeGreen
                percentage >= 60 -> CyberBlue
                else -> WarningOrange
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = when {
                percentage >= 80 -> "Guardian Master"
                percentage >= 60 -> "Digital Defender"
                else -> "Keep Learning"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Score: $score / $maxScore",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "($percentage%)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                percentage >= 80 -> "Excellent! You have strong scam detection skills. Cameroon is safer with users like you."
                percentage >= 60 -> "Good job! You're developing solid digital intuition. Keep practicing to stay ahead of scammers."
                else -> "Good effort! Scammers are constantly evolving. Practice more to become a digital patriot."
            },
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = CyberBlue.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Difficulty: ${difficulty.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Try harder difficulties to test your skills!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete Training", fontWeight = FontWeight.Bold)
        }
    }
}

private fun getScenarios(difficulty: Difficulty): List<Scenario> {
    val baseScenarios = listOf(
        Scenario(
            id = "momo-fraud",
            title = "MTN MoMo Message",
            content = "MTN: You have received 50,000 XAF from 677123456. Transaction ID: MTN-2024-001. To confirm, please dial *126# and enter your PIN to claim your money.",
            isScam = true,
            scamType = ScamType.FINANCIAL,
            difficulty = Difficulty.BEGINNER,
            explanation = "This is a classic MoMo fraud attempt. MTN will NEVER ask you to dial a code and enter your PIN to receive money. The PIN is only for sending money, not receiving.",
            redFlags = listOf(
                "Asking for PIN via USSD code",
                "Urgency to claim money",
                "Fake transaction ID format",
                "MTN doesn't use this format for notifications"
            ),
            icon = Icons.Filled.AccountBalanceWallet
        ),
        Scenario(
            id = "job-scam",
            title = "WhatsApp Job Offer",
            content = "Hi! I'm HR manager at Amazon Cameroon. We saw your profile and want to offer you a remote job earning 150,000 XAF/day. Click here to apply: bit.ly/amazon-jobs-cm2024",
            isScam = true,
            scamType = ScamType.JOB_SCAM,
            difficulty = Difficulty.BEGINNER,
            explanation = "This is a job scam. Real companies don't recruit via WhatsApp with unrealistic salaries. The URL is a shortened link (bit.ly) which hides the real destination.",
            redFlags = listOf(
                "Unrealistic salary (150,000 XAF/day)",
                "Recruitment via WhatsApp",
                "Shortened URL (bit.ly)",
                "No official company email",
                "Poor grammar and informal tone"
            ),
            icon = Icons.Filled.Work
        ),
        Scenario(
            id = "bank-phishing",
            title = "Afriland Bank Alert",
            content = "Afriland First Bank: Your account has been temporarily locked due to unusual activity. Please verify your identity immediately at https://afriland-security-verify.net/login",
            isScam = true,
            scamType = ScamType.PHISHING,
            difficulty = Difficulty.INTERMEDIATE,
            explanation = "This is a phishing attempt. The URL is fake - real banks use their official domain (afrilandfirstbank.com), not afriland-security-verify.net. Always verify URLs before clicking.",
            redFlags = listOf(
                "Fake URL (not official bank domain)",
                "Urgency ('immediately')",
                "Threat of account lockout",
                "Generic greeting",
                "No personal account details mentioned"
            ),
            icon = Icons.Filled.AccountBalance
        ),
        Scenario(
            id = "lottery-scam",
            title = "National Lottery Win",
            content = "CONGRATULATIONS! You've won 5,000,000 XAF in the Cameroon National Lottery. To claim, send your full name, phone number, and address to +237 6XX XXX XXX. Processing fee: 10,000 XAF.",
            isScam = true,
            scamType = ScamType.LOTTERY,
            difficulty = Difficulty.BEGINNER,
            explanation = "This is a lottery scam. You cannot win a lottery you didn't enter. Legitimate lotteries don't ask for processing fees or personal info via WhatsApp.",
            redFlags = listOf(
                "You didn't enter any lottery",
                "Request for processing fee (10,000 XAF)",
                "Request for personal information",
                "Unofficial contact method",
                "Too good to be true"
            ),
            icon = Icons.Filled.EmojiEvents
        ),
        Scenario(
            id = "romance-scam",
            title = "Romance Message",
            content = "Hello dear, I saw your profile and felt a connection. I'm a doctor working with WHO in Douala. I'm looking for a serious relationship. Can we chat on WhatsApp? +237 6XX XXX XXX",
            isScam = true,
            scamType = ScamType.ROMANCE,
            difficulty = Difficulty.INTERMEDIATE,
            explanation = "This is a romance scam. Scammers create fake profiles, often claiming to be professionals (doctors, engineers) working abroad. They build trust before asking for money.",
            redFlags = listOf(
                "Immediate emotional connection",
                "Claims to be professional (doctor)",
                "Moving to WhatsApp quickly",
                "No mutual friends or connections",
                "Too good to be true"
            ),
            icon = Icons.Filled.Favorite
        ),
        Scenario(
            id = "govt-impersonation",
            title = "Tax Authority Message",
            content = "Direction Générale des Impôts: You have a tax refund of 250,000 XAF. Download the attached form 'tax-refund.apk' to process your payment immediately.",
            isScam = true,
            scamType = ScamType.IMPERSONATION,
            difficulty = Difficulty.ADVANCED,
            explanation = "This is government impersonation with malware. Tax authorities never send .apk files. The file is likely malware designed to steal your data. Legitimate refunds are processed through official channels.",
            redFlags = listOf(
                "Government agency sending .apk file",
                "APK files can contain malware",
                "Urgency ('immediately')",
                "No official letterhead or reference number",
                "Unusual method for tax refunds"
            ),
            icon = Icons.Filled.AccountBalance
        ),
        Scenario(
            id = "tech-support",
            title = "Microsoft Support Call",
            content = "Hello, this is Microsoft Support. We detected suspicious activity on your Windows device. Please download this app to let us fix it: support-tool.xyz",
            isScam = true,
            scamType = ScamType.TECH_SUPPORT,
            difficulty = Difficulty.ADVANCED,
            explanation = "This is a tech support scam. Microsoft never calls users unsolicited. The URL is fake, and the 'app' is likely malware or remote access software.",
            redFlags = listOf(
                "Unsolicited call from 'Microsoft'",
                "Fake URL (support-tool.xyz)",
                "Request to download unknown app",
                "Fear tactics ('suspicious activity')",
                "Microsoft doesn't make support calls"
            ),
            icon = Icons.Filled.SupportAgent
        ),
        Scenario(
            id = "legitimate-bank",
            title = "Real Bank Notification",
            content = "SGBC: Your account ending in 4521 was debited 25,000 XAF at CARREFOUR DOUALA on 04/07/2024 at 14:32. Balance: 145,000 XAF. Reply STOP to unsubscribe.",
            isScam = false,
            scamType = ScamType.FINANCIAL,
            difficulty = Difficulty.INTERMEDIATE,
            explanation = "This appears to be a legitimate bank notification. It includes specific transaction details (amount, location, time, account number) and provides an unsubscribe option. Real bank SMS notifications are detailed and specific.",
            redFlags = listOf(
                "None - this appears legitimate",
                "Contains specific transaction details",
                "Shows partial account number",
                "Includes date and time",
                "Provides unsubscribe option"
            ),
            icon = Icons.Filled.CheckCircle
        ),
        Scenario(
            id = "family-emergency",
            title = "Family Emergency",
            content = "MOM: I lost my phone and this is my new number. I'm at the hospital and need 100,000 XAF for emergency treatment. Please send to 677XXXXXX immediately. It's urgent!",
            isScam = true,
            scamType = ScamType.URGENT_ACTION,
            difficulty = Difficulty.ADVANCED,
            explanation = "This is a family emergency scam (also called 'grandparent scam'). Scammers impersonate family members claiming emergencies. Always verify through another channel before sending money.",
            redFlags = listOf(
                "Claims to have 'lost phone'",
                "Urgency ('immediately', 'urgent')",
                "Request for large sum",
                "No specific hospital name",
                "Emotional manipulation"
            ),
            icon = Icons.Filled.Warning
        ),
        Scenario(
            id = "crypto-scam",
            title = "Crypto Investment",
            content = "🚀 BITCOIN OPPORTUNITY: Invest 50,000 XAF and earn 500,000 XAF in 24 hours! Limited spots remaining. Join our WhatsApp group: https://chat.whatsapp.com/XXXXXXXXXX",
            isScam = true,
            scamType = ScamType.FINANCIAL,
            difficulty = Difficulty.INTERMEDIATE,
            explanation = "This is a cryptocurrency investment scam. Guaranteed high returns in short time are impossible. These are Ponzi schemes that pay early investors with money from new victims.",
            redFlags = listOf(
                "Guaranteed high returns (10x in 24h)",
                "Urgency ('limited spots')",
                "WhatsApp group invitation",
                "Too good to be true",
                "No legitimate investment details"
            ),
            icon = Icons.Filled.TrendingUp
        )
    )
    
    return when (difficulty) {
        Difficulty.BEGINNER -> baseScenarios.filter { it.difficulty == Difficulty.BEGINNER }.take(5)
        Difficulty.INTERMEDIATE -> baseScenarios.filter { 
            it.difficulty == Difficulty.BEGINNER || it.difficulty == Difficulty.INTERMEDIATE 
        }.take(5)
        Difficulty.ADVANCED -> baseScenarios.take(5)
    }
}
