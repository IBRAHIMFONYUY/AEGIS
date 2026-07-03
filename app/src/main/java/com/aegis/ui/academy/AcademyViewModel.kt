package com.aegis.ui.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        viewModelScope.launch {
            learningRepository.getCompletedCount().collect { count ->
                _completedCount.value = count
            }
        }
        
        viewModelScope.launch {
            learningRepository.getAllModules().collect { progressList ->
                val currentModules = _modules.value.map { module ->
                    module.copy(progress = progressList.find { it.moduleId == module.id })
                }
                _modules.value = currentModules
                _totalScore.value = progressList.sumOf { it.score }
            }
        }
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
}
