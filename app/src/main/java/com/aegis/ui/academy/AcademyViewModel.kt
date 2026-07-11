package com.aegis.ui.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.data.academy.*
import com.aegis.agents.GuardianCore
import com.aegis.data.db.entity.LearningProgress
import com.aegis.data.repository.LearningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AcademyViewModel @Inject constructor(
    private val learningRepository: LearningRepository,
    private val guardianCore: GuardianCore
) : ViewModel() {

    data class AcademyModule(
        val id: String,
        val title: String,
        val description: String,
        val icon: String,
        val difficulty: String,
        val estimatedMinutes: Int,
        val progress: LearningProgress? = null
    )

    private val _modules = MutableStateFlow(defaultModules())
    val modules: StateFlow<List<AcademyModule>> = _modules.asStateFlow()

    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount.asStateFlow()

    private val _totalScore = MutableStateFlow(0)
    val totalScore: StateFlow<Int> = _totalScore.asStateFlow()

    private val _currentTopic = MutableStateFlow<AcademyTopic?>(null)
    val currentTopic = _currentTopic.asStateFlow()

    private val _currentLevel = MutableStateFlow(AcademyLevel.BEGINNER)
    val currentLevel = _currentLevel.asStateFlow()

    private val _currentScenario = MutableStateFlow<AcademyScenario?>(null)
    val currentScenario = _currentScenario.asStateFlow()

    private val _lessonProgress = MutableStateFlow(0f)
    val lessonProgress = _lessonProgress.asStateFlow()

    data class QuizResult(
        val correctAnswers: Int,
        val totalQuestions: Int,
        val pointsEarned: Int,
        val reviewItems: List<ReviewItem>,
        val rankUpgrade: String? = null
    )

    data class ReviewItem(
        val question: String,
        val userAnswer: String,
        val correctAnswer: String,
        val isCorrect: Boolean,
        val explanation: String
    )

    private val _quizResult = MutableStateFlow<QuizResult?>(null)
    val quizResult = _quizResult.asStateFlow()

    private var currentSessionCorrectCount = 0
    private var currentSessionTotalCount = 0
    private var currentSessionPoints = 0
    private val currentSessionReviewItems = mutableListOf<ReviewItem>()

    init {
        // ... previous init code
    }

    fun selectTopic(topic: AcademyTopic) {
        _currentTopic.value = topic
        startLesson(topic.id, AcademyLevel.BEGINNER)
    }

    fun startLesson(topicId: String, level: AcademyLevel) {
        _quizResult.value = null
        currentSessionCorrectCount = 0
        currentSessionTotalCount = 0
        currentSessionPoints = 0
        currentSessionReviewItems.clear()
        
        val scenarios = AcademyContent.generateScenarios()
            .filter { it.topicId == topicId && it.level == level }
        
        _currentScenario.value = scenarios.firstOrNull()
        _lessonProgress.value = 0f
    }

    fun submitAnswer(optionIndex: Int) {
        val scenario = _currentScenario.value ?: return
        currentSessionTotalCount++
        
        val isCorrect = optionIndex == scenario.correctOptionIndex
        val userAnswer = scenario.options.getOrElse(optionIndex) { "Unknown" }
        val correctAnswer = scenario.options[scenario.correctOptionIndex]

        currentSessionReviewItems.add(
            ReviewItem(
                question = scenario.question,
                userAnswer = userAnswer,
                correctAnswer = correctAnswer,
                isCorrect = isCorrect,
                explanation = scenario.explanation
            )
        )

        if (isCorrect) {
            currentSessionCorrectCount++
            currentSessionPoints += scenario.points
            
            viewModelScope.launch {
                learningRepository.completeModule(scenario.id, 100)
                _totalScore.value += scenario.points
            }
        }
        
        moveToNextScenario(scenario)
    }

    private fun moveToNextScenario(current: AcademyScenario) {
        val allScenarios = AcademyContent.generateScenarios()
            .filter { it.topicId == current.topicId && it.level == current.level }
        
        val currentIndex = allScenarios.indexOf(current)
        if (currentIndex < allScenarios.size - 1) { // Process all 50 scenarios
            _currentScenario.value = allScenarios[currentIndex + 1]
            _lessonProgress.value = (currentIndex + 1).toFloat() / allScenarios.size
        } else {
            // Quiz Complete!
            _currentScenario.value = null
            _quizResult.value = QuizResult(
                correctAnswers = currentSessionCorrectCount,
                totalQuestions = currentSessionTotalCount,
                pointsEarned = currentSessionPoints,
                reviewItems = currentSessionReviewItems.toList()
            )
        }
    }

    fun finishQuiz() {
        val result = _quizResult.value
        if (result != null) {
            // Record academy success in global analytics
            viewModelScope.launch {
                val context = com.aegis.core.AnalysisContext(
                    text = "Academy Quiz Completed: ${currentTopic.value?.title}",
                    sourceType = com.aegis.core.SourceType.UNKNOWN,
                    metadata = mapOf(
                        "academy_score" to result.pointsEarned.toString(),
                        "academy_accuracy" to (result.correctAnswers.toFloat() / result.totalQuestions).toString()
                    )
                )
                guardianCore.analyze(context)
            }
        }
        _quizResult.value = null
        _currentTopic.value = null
    }

    private fun defaultModules() = listOf(
        AcademyModule("phishing_101", "Phishing Basics", "Learn how to spot common phishing attempts.", "hook", "Beginner", 10),
        AcademyModule("scam_calls", "Scam Call Defense", "Identify and handle fraudulent phone calls.", "phone", "Beginner", 15),
        AcademyModule("malware_aware", "Malware Awareness", "Protect your device from malicious apps.", "bug", "Intermediate", 20),
        AcademyModule("privacy_settings", "Data Privacy", "Master your Android privacy settings.", "lock", "Intermediate", 15),
        AcademyModule("ai_security", "Future of AI Security", "How AI helps and hurts cybersecurity.", "robot", "Advanced", 30)
    )

    fun startModule(moduleId: String) {
        // Implementation for starting a module
    }

    fun completeModule(moduleId: String, score: Int = 100) {
        viewModelScope.launch {
            learningRepository.completeModule(moduleId, score)
        }
    }
}
