package com.aegis.ui.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.agents.GuardianCore
import com.aegis.data.db.entity.LearningProgress
import com.aegis.data.repository.LearningRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AcademyViewModel(
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

    init {
        viewModelScope.launch {
            learningRepository.getCompletedCount().collect { count ->
                _completedCount.value = count
            }
        }
        viewModelScope.launch {
            learningRepository.getTotalScore().collect { score ->
                _totalScore.value = score ?: 0
            }
        }
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            learningRepository.getAllModules().collect { dbModules ->
                _modules.value = defaultModules().map { module ->
                    val dbProgress = dbModules.find { it.moduleId == module.id }
                    module.copy(progress = dbProgress)
                }
            }
        }
    }

    fun completeModule(moduleId: String, score: Int) {
        viewModelScope.launch {
            learningRepository.completeModule(moduleId, score)
        }
    }

    private fun defaultModules() = listOf(
        AcademyModule(
            id = "phishing_101",
            title = "Phishing 101",
            description = "Learn to identify and avoid phishing attempts",
            icon = "🎣",
            difficulty = "Beginner",
            estimatedMinutes = 10
        ),
        AcademyModule(
            id = "password_security",
            title = "Password Security",
            description = "Create strong passwords and manage them safely",
            icon = "🔑",
            difficulty = "Beginner",
            estimatedMinutes = 8
        ),
        AcademyModule(
            id = "social_engineering",
            title = "Social Engineering",
            description = "Understand how attackers manipulate people",
            icon = "🎭",
            difficulty = "Intermediate",
            estimatedMinutes = 15
        ),
        AcademyModule(
            id = "safe_browsing",
            title = "Safe Browsing",
            description = "Browse the web safely and avoid malicious sites",
            icon = "🌐",
            difficulty = "Beginner",
            estimatedMinutes = 10
        ),
        AcademyModule(
            id = "mobile_security",
            title = "Mobile Security",
            description = "Secure your smartphone against threats",
            icon = "📱",
            difficulty = "Intermediate",
            estimatedMinutes = 12
        ),
        AcademyModule(
            id = "scam_awareness",
            title = "Scam Awareness",
            description = "Recognize common scam tactics and protect yourself",
            icon = "⚠️",
            difficulty = "Beginner",
            estimatedMinutes = 10
        ),
        AcademyModule(
            id = "privacy_matters",
            title = "Privacy Matters",
            description = "Protect your personal data and privacy online",
            icon = "🔒",
            difficulty = "Intermediate",
            estimatedMinutes = 15
        ),
        AcademyModule(
            id = "incident_response",
            title = "Incident Response",
            description = "What to do when you encounter a security threat",
            icon = "🚨",
            difficulty = "Advanced",
            estimatedMinutes = 20
        ),
        AcademyModule(
            id = "digital_patriot",
            title = "Digital Patriot",
            description = "Become a champion of cybersecurity in your community",
            icon = "🛡️",
            difficulty = "Advanced",
            estimatedMinutes = 25
        ),
        AcademyModule(
            id = "misinformation",
            title = "Fake News Detection",
            description = "Learn to spot misinformation and verify facts",
            icon = "📰",
            difficulty = "Intermediate",
            estimatedMinutes = 15
        )
    )
}
