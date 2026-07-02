package com.aegis.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_progress")
data class LearningProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: String,
    val moduleName: String,
    val completed: Boolean = false,
    val score: Int = 0,
    val maxScore: Int = 100,
    val quizzesPassed: Int = 0,
    val achievements: String? = null,
    val lastAccessed: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
