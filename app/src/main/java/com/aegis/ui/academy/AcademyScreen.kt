package com.aegis.ui.academy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.data.academy.*
import com.aegis.agents.GuardianCore
import com.aegis.data.repository.LearningRepository
import com.aegis.ui.components.*
import com.aegis.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyScreen(
    learningRepository: LearningRepository,
    guardianCore: GuardianCore,
    navController: androidx.navigation.NavController? = null,
    viewModel: AcademyViewModel = hiltViewModel()
) {
    val modules by viewModel.modules.collectAsState()
    val totalScore by viewModel.totalScore.collectAsState()
    val currentScenario by viewModel.currentScenario.collectAsState()
    val lessonProgress by viewModel.lessonProgress.collectAsState()
    val quizResult by viewModel.quizResult.collectAsState()

    if (quizResult != null) {
        QuizResultUI(result = quizResult!!, onFinish = { viewModel.finishQuiz() })
    } else if (currentScenario != null) {
        LessonUI(
            scenario = currentScenario!!,
            progress = lessonProgress,
            onAnswer = { viewModel.submitAnswer(it) },
            onQuit = { viewModel.finishQuiz() }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Digital Patriot Academy", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    GradientTopBar()
                }

                item {
                    ScoreBoard(totalScore)
                }

                item {
                    SectionHeader("Interactive Topics")
                }

                items(AcademyContent.topics) { topic ->
                    TopicCard(topic) { viewModel.selectTopic(topic) }
                }

                item {
                    SectionHeader("Traditional Modules")
                }

                items(modules) { module ->
                    ModuleListItem(module) { viewModel.completeModule(module.id, 100) }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ScoreBoard(score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = AegisPrimary),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ACADEMY XP", color = Color.Black.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge)
                Text(score.toString(), color = Color.Black, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("RANK", color = Color.Black.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge)
                Text(if (score > 1000) "GUARDIAN" else "RECRUIT", color = Color.Black, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TopicCard(topic: AcademyTopic, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(topic.icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(topic.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(topic.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Filled.ArrowForwardIos, null, modifier = Modifier.size(16.dp), tint = Color.LightGray)
        }
    }
}

@Composable
fun ModuleListItem(module: AcademyViewModel.AcademyModule, onClick: () -> Unit) {
    val isCompleted = module.progress?.completed == true
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (isCompleted) SafeGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(module.icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(module.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(module.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (isCompleted) Icon(Icons.Filled.CheckCircle, null, tint = SafeGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun QuizResultUI(result: AcademyViewModel.QuizResult, onFinish: () -> Unit) {
    var showReview by remember { mutableStateOf(false) }

    if (showReview) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showReview = false }) { Icon(Icons.Filled.ArrowBack, null) }
                Text("DETAILED ANSWERS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(result.reviewItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.question, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your Answer: ${item.userAnswer}", color = if (item.isCorrect) SafeGreen else DangerRed)
                            if (!item.isCorrect) {
                                Text("Correct Answer: ${item.correctAnswer}", color = SafeGreen)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Evaluation:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = AegisPrimary)
                            Text(item.explanation, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimary)
            ) {
                Text("DONE", fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("QUIZ COMPLETE!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = AegisPrimary)
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(label = "Correct", value = "${result.correctAnswers}/${result.totalQuestions}", color = SafeGreen)
                        StatItem(label = "XP Earned", value = "+${result.pointsEarned}", color = AegisPrimary)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    val percentage = (result.correctAnswers.toFloat() / result.totalQuestions * 100).toInt()
                    Text("Accuracy: $percentage%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { showReview = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, AegisPrimary)
            ) {
                Text("REVIEW ANSWERS", fontWeight = FontWeight.ExtraBold, color = AegisPrimary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AegisPrimary)
            ) {
                Text("CONTINUE TO ACADEMY", fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonUI(
    scenario: AcademyScenario,
    progress: Float,
    onAnswer: (Int) -> Unit,
    onQuit: () -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var showExplanation by remember { mutableStateOf(false) }
    val scrollState = androidx.compose.foundation.rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQuit) { Icon(Icons.Filled.Close, null) }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .padding(horizontal = 12.dp),
                color = AegisPrimary,
                trackColor = AegisPrimary.copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Text("💎 ${scenario.points}", fontWeight = FontWeight.Bold, color = AegisPrimary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Make the content scrollable
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                "SCENARIO",
                style = MaterialTheme.typography.labelLarge,
                color = AegisPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                scenario.question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            scenario.options.forEachIndexed { index, option ->
                val isCorrect = index == scenario.correctOptionIndex
                val isSelected = index == selectedIndex
                val containerColor = when {
                    showExplanation && isCorrect -> SafeGreen
                    showExplanation && isSelected && !isCorrect -> DangerRed
                    isSelected -> AegisPrimary.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surface
                }
                val borderColor = if (isSelected) AegisPrimary else MaterialTheme.colorScheme.outlineVariant

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = { if (!showExplanation) selectedIndex = index },
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(20.dp),
                        fontWeight = FontWeight.Bold,
                        color = if (showExplanation && (isCorrect || (isSelected && !isCorrect))) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (showExplanation) {
                Spacer(modifier = Modifier.height(24.dp))
                val isCorrect = selectedIndex == scenario.correctOptionIndex
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = (if (isCorrect) SafeGreen else DangerRed).copy(alpha = 0.1f)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            null,
                            tint = if (isCorrect) SafeGreen else DangerRed
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (isCorrect) "EXCELLENT!" else "NOT QUITE",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCorrect) SafeGreen else DangerRed
                            )
                            Text(scenario.explanation, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (showExplanation) {
                    onAnswer(selectedIndex)
                    showExplanation = false
                    selectedIndex = -1
                } else if (selectedIndex != -1) {
                    showExplanation = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedIndex != -1,
            colors = ButtonDefaults.buttonColors(containerColor = AegisPrimary),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Text(if (showExplanation) "CONTINUE" else "CHECK", fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }
    }
}
