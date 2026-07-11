package com.aegis.data.academy

enum class AcademyLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}

data class AcademyTopic(
    val id: String,
    val title: String,
    val description: String,
    val icon: String
)

data class AcademyScenario(
    val id: String,
    val topicId: String,
    val level: AcademyLevel,
    val question: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
    val points: Int = 10
)

data class LessonProgress(
    val topicId: String,
    val level: AcademyLevel,
    val completedScenarioIds: Set<String>,
    val totalPoints: Int
)
